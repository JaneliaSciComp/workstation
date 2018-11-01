package org.janelia.it.workstation.browser.filecache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalListeners;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.api.http.HttpClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages a local file cache with a defined physical
 * storage capacity.  It is designed to support fast concurrent access.
 *
 * @author Eric Trautman
 */
public class LocalFileCache {

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileCache.class);

    private static final String CACHE_DIRECTORY_NAME = ".jacs-file-cache";
    private static final String ACTIVE_DIRECTORY_NAME = "active";
    private static final String TEMP_DIRECTORY_NAME = "temp";

    private File tempDirectory;
    private File activeDirectory;

    private long kilobyteCapacity;

    private final ExecutorService asyncLoadService;
    private final Weigher<String, CachedFile> weigher;
    private final RemovalListener<String, CachedFile> asyncRemovalListener;
    private final StorageClientMgr storageClientMgr;
    private final RemoteFileCacheLoader defaultLoader;
    private LoadingCache<String, CachedFile> remoteNameToFileCache;

    private final CacheLoadEventListener cacheLoadEventListener;

    /**
     * Creates a new local cache whose physical storage is within the
     * specified parent directory. The cache uses a Least Recently Used
     * algorithm to cull files from the physical directory once the
     * specified capacity (in kilobytes) is reached.
     *
     * @param  cacheParentDirectory  parent directory for the physical cache.
     *
     * @param  kilobyteCapacity      number of kilobytes to allow in the
     *                               physical cache before removing least
     *                               recently used files.
     *
     * @param  cacheLoadEventListener  listener for cache load events
     *                                 (or null if not needed).
     *
     * @param httpClient for loading remote files
     *
     * @param storageClientMgr for finding remote files
     *
     * @throws IllegalStateException
     *   if any errors occur while constructing a cache tied to the file system.
     */
    public LocalFileCache(File cacheParentDirectory,
                          long kilobyteCapacity,
                          CacheLoadEventListener cacheLoadEventListener,
                          HttpClientProxy httpClient,
                          StorageClientMgr storageClientMgr)
            throws IllegalStateException {

        File cacheRootDirectory = createAndValidateDirectoryAsNeeded(cacheParentDirectory, CACHE_DIRECTORY_NAME);
        this.activeDirectory = createAndValidateDirectoryAsNeeded(cacheRootDirectory, ACTIVE_DIRECTORY_NAME);
        this.tempDirectory = createAndValidateDirectoryAsNeeded(cacheRootDirectory, TEMP_DIRECTORY_NAME);

        if (kilobyteCapacity < 1) {
            this.kilobyteCapacity = 1;
        } else {
            this.kilobyteCapacity = kilobyteCapacity;
        }

        this.cacheLoadEventListener = cacheLoadEventListener;

        // separate thread pool for async addition of files to the cache
        this.asyncLoadService = Executors.newFixedThreadPool(4);

        this.weigher = new Weigher<String, CachedFile>() {

            @Override
            public int weigh(String remoteFileRefName, CachedFile value) {

                long kiloBytes = value.getKilobytes();

                // doubt we'll ever have > 2000 gigabyte file,
                // but if so it simply won't be fairly weighted
                if (kiloBytes > Integer.MAX_VALUE) {
                    LOG.warn("weightOf: truncating weight for " + kiloBytes + " Kb file " + value);
                    kiloBytes = Integer.MAX_VALUE;
                } else if (kiloBytes == 0) {
                    // zero weights are not supported,
                    // so we need to set empty file weight to 1
                    kiloBytes = 1;
                }
                return (int) kiloBytes;
            }
        };

        this.asyncRemovalListener =
                RemovalListeners.asynchronous(
                        new RemovalListener<String, CachedFile>() {
                            @Override
                            public void onRemoval(RemovalNotification<String, CachedFile> removal) {
                                final CachedFile cachedFile = removal.getValue();
                                if (cachedFile != null) {
                                    cachedFile.remove(getActiveDirectory());
                                }
                            }
                        },
                        Executors.newFixedThreadPool(4)); // separate thread pool for removing files that expire from the cache

        this.storageClientMgr = storageClientMgr;
        this.defaultLoader = new RemoteFileCacheLoader(httpClient, storageClientMgr, this);

        final File[] tempFiles = tempDirectory.listFiles();
        if ((tempFiles != null) && (tempFiles.length > 0)) {

            // TODO: decide whether to clear-out temp cache directory at start-up

            // The directory should be empty unless a load from a prior session was interrupted.
            // Could just remove anything in temp at start-up (on a separate thread),
            // but need to consider/handle possibility of concurrent load.
            // Taking the easy way out at this point by just logging the anomaly.

            LOG.warn("temp directory {} should be empty but contains {} files",
                     tempDirectory.getAbsolutePath(), tempFiles.length);
        }

        this.buildCacheAndScheduleLoad();

        LOG.debug("<init>: exit");
    }

    /**
     * @return the directory where cached files can be loaded
     *         before they are ready to be served from the cache.
     */
    File getTempDirectory() {
        return tempDirectory;
    }

    /**
     * @return the directory that contains all locally cached files
     *         that are ready to be served.
     */
    File getActiveDirectory() {
        return activeDirectory;
    }

    /**
     * @return the number of files currently in the cache.
     */
    public long getNumberOfFiles() {
        return remoteNameToFileCache.size();
    }

    /**
     * Dynamically calculates the total cache size by examining each
     * cached file.  Do not call this method inside performance
     * sensitive blocks.
     *
     * @return the number of kilobytes currently stored in the cache.
     */
    public long getNumberOfKilobytes() {
        long weightedSize = 0;
        // loop through values without affecting recency ordering
        final Map<String, CachedFile> internalCacheMap = remoteNameToFileCache.asMap();
        for (CachedFile cachedFile : internalCacheMap.values()) {
            weightedSize += weigher.weigh(null, cachedFile);
        }
        return weightedSize;
    }

    /**
     * @return the maximum number of kilobytes to be maintained in this cache.
     */
    public long getKilobyteCapacity() {
        return kilobyteCapacity;
    }

    /**
     * Sets the maximum number of kilobytes to be maintained in this cache
     * and then rebuilds the cache.  If the current cache size exceeds the
     * new maximum, items will be scheduled for eviction BUT they won't
     * necessarily be least recently used since this operation requires that
     * the entire cache be rebuilt.
     * This is potentially an expensive operation, so use it wisely.
     *
     * @param  kilobyteCapacity  maximum cache capacity in kilobytes.
     */
    public void setKilobyteCapacity(long kilobyteCapacity) {
        LOG.info("setKilobyteCapacity: entry, kilobyteCapacity={}", kilobyteCapacity);
        this.kilobyteCapacity = kilobyteCapacity;
        buildCacheAndScheduleLoad();
    }

    /**
     * Looks for the specified resource in the cache and returns the
     * corresponding local file copy.
     * If the resource is not in the cache, it is retrieved/copied
     * (on the current thread of execution) and is added to the
     * cache before being returned.
     *
     * @param  remoteFileRefName  remote file reference name.
     *
     * @param  forceRefresh   if true, will force removal of any existing
     *                        cached file before retrieving it again from
     *                        the remote source.
     *
     * @return the local cached instance of the specified remote file.
     *
     * @throws FileNotCacheableException
     *   if the file cannot be cached locally.
     */
    public File getFile(String remoteFileRefName, boolean forceRefresh)
            throws FileNotCacheableException, FileNotFoundException {
        File localFile;
        try {
            if (forceRefresh) {
                remoteNameToFileCache.invalidate(remoteFileRefName);
            }
            // get call should load file if it is not already present
            CachedFile cachedFile = remoteNameToFileCache.get(remoteFileRefName);
            localFile = getVerifiedLocalFile(cachedFile);
        } 
        catch (Exception e) {
            Throwable cause = e;
            while(cause != null) {
                if (FileNotFoundException.class.isAssignableFrom(cause.getClass())) {
                    throw new FileNotFoundException(remoteFileRefName);
                }
                cause = cause.getCause();
            }
            throw new FileNotCacheableException("failed to retrieve " + remoteFileRefName, e);
        }

        if (localFile == null) {
            throw new FileNotCacheableException("local cache file missing for " + remoteFileRefName);
        }

        return localFile;
    }

    /**
     * Looks for the specified resource in the cache and returns the
     * corresponding local file URL if it exists.
     * If the resource is not in the cache, the specified remote URL
     * is immediately returned and an asynchronous request is submitted
     * to cache the resource.
     *
     * @param  remoteFileRefName  remote reference file name
     *
     * @param cacheAsync to load the file
     *
     * @return the local or remote URL for the resource depending upon
     *         whether it has already been cached.
     */
    public URLProxy getEffectiveUrl(String remoteFileRefName, boolean cacheAsync) throws FileNotFoundException {
        // get call will NOT load file if it is missing
        CachedFile cachedFile = remoteNameToFileCache.getIfPresent(remoteFileRefName);
        File localFile = getVerifiedLocalFile(cachedFile);

        if (localFile == null) {
            if (cacheAsync) {
                if (remoteFileRefName.endsWith("/")) {
                    LOG.trace("Cannot cache directory: "+remoteFileRefName);
                }
                else {   
                    asyncLoadService.submit(new Callable<File>() {
                        @Override
                        public File call() throws Exception {
                            try {
                                return getFile(remoteFileRefName, false);
                            } catch (FileNotCacheableException e) {
                                LOG.warn("Problem encountered caching file asynchronously",e);
                                throw e;
                            }
                        }
                    });
                }
            }
            return storageClientMgr.getDownloadFileURL(remoteFileRefName);
        } else  {
            try {
                return new URLProxy(localFile.toURI().toURL());
            }  catch (MalformedURLException e) {
                LOG.error("failed to derive URL for " + localFile.getAbsolutePath(), e);
                throw new IllegalStateException("Invalid cached file URL " + localFile, e);
            }
        }
    }

    /**
     * Clears and removes all locally cached files.
     * Entries will be removed from the in-memory metadata cache immediately.
     * The locally cached files will be removed from the file system
     * asynchronously by a separate pool of threads.
     */
    public void clear() {
        LOG.info("clear: entry, scheduling removal of {} files from cache", remoteNameToFileCache.size());
        remoteNameToFileCache.invalidateAll();
    }

    @Override
    public String toString() {
        return "LocalFileCache{" +
                "activeDirectory=" + activeDirectory +
                ", tempDirectory=" + tempDirectory +
                ", kilobyteCapacity=" + kilobyteCapacity +
                '}';
    }

    /**
     * Ensures that a directory with the specified name exists within the
     * specified parent directory that is writable.
     *
     * @param  parent  parent directory for the directory.
     * @param  name    name of the directory.
     *
     * @return a validated directory instance.
     *
     * @throws IllegalStateException
     *   if the directory cannot be created or is not writable.
     */
    private File createAndValidateDirectoryAsNeeded(File parent, String name)
            throws IllegalStateException {

        File canonicalParent;
        try {
            canonicalParent = parent.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalStateException("failed to derive canonical name for " +
                                            parent.getAbsolutePath(), e);
        }

        File directory = new File(canonicalParent, name);
        if (! directory.exists()) {
            if (! directory.mkdirs()) {
                throw new IllegalStateException("cannot create " + directory.getAbsolutePath());
            }
        }

        if (! directory.canWrite()) {
            throw new IllegalStateException("cannot write to " + directory.getAbsolutePath());
        }

        return directory;
    }

    /**
     * Builds a new empty cache using the current capacity and then
     * launches a separate thread to load the cache from the filesystem.
     */
    private void buildCacheAndScheduleLoad() {

        // Setting concurrency level to 1 ensures global LRU eviction
        // by limiting all entries to one segment
        // (see http://stackoverflow.com/questions/10236057/guava-cache-eviction-policy ).
        // The "penalty" for this appears to be serialzed put of the object
        // AFTER it has been loaded - which should not be a problem.

        this.remoteNameToFileCache =
                CacheBuilder.newBuilder()
                        .concurrencyLevel(1)
                        .maximumWeight(getKilobyteCapacity())
                        .weigher(weigher)
                        .removalListener(asyncRemovalListener)
                        .build(defaultLoader);

        // load cache from a separate thread so that we don't
        // bog down application start up
        Thread loadThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        loadCacheFromFilesystem();
                    }
                }, "local-file-cache-load-thread");

        loadThread.start();

    }

    /**
     * Registers any existing local files in this cache.
     *
     * NOTE: After load, cache usage (ordering) will simply reflect
     *       directory traversal order.
     */
    private void loadCacheFromFilesystem() {

        LOG.info("loadCacheFromFilesystem: starting load");

        LocalFileLoader loader = new LocalFileLoader(activeDirectory);
        final List<CachedFile> cachedFiles = loader.locateCachedFiles();
        for (CachedFile cachedFile : cachedFiles) {
            // make sure newer cache record has not already been loaded
            if (!StringUtils.isBlank(cachedFile.getRemoteFileName())) {
                if (remoteNameToFileCache.getIfPresent(cachedFile.getRemoteFileName()) == null) {
                    remoteNameToFileCache.put(cachedFile.getRemoteFileName(), cachedFile);
                }
            } else {
                // this is a legacy file - remove it from the active directory
                cachedFile.remove(this.activeDirectory);
            }
        }

        final long usedKb = getNumberOfKilobytes();
        final long totalKb = getKilobyteCapacity();
        final int usedPercentage = (int)
                (((double) usedKb / (double) totalKb) * 100);

        LOG.info("loadCacheFromFilesystem: loaded " + cachedFiles.size() +
                " files into " + this +
                ", " + usedPercentage + "% full (" + getNumberOfKilobytes() + "/" +
                getKilobyteCapacity() + " kilobytes)");

        if (cacheLoadEventListener != null) {
            cacheLoadEventListener.loadCompleted(loader.getUnregisteredFiles());
        }
    }

    private File getVerifiedLocalFile(CachedFile cachedFile) {
        // extra check to ensure cache is consistent with filesystem - maybe overkill?
        File localFile = null;
        if (cachedFile != null) {
            localFile = cachedFile.getLocalFile();
            if (!localFile.exists()) {
                remoteNameToFileCache.invalidate(cachedFile.getRemoteFileName());
                localFile = null;
            }
        }
        return localFile;
    }

}
