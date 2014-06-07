package org.janelia.it.workstation.shared.util.filecache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalListeners;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class manages a local file cache with a defined physical
 * storage capacity.  It is designed to support fast concurrent access.
 *
 * @author Eric Trautman
 */
public class LocalFileCache {

    private File rootDirectory;
    private File tempDirectory;
    private File activeDirectory;

    private long kilobyteCapacity;

    private WebDavClient webDavClient;
    private ExecutorService asyncLoadService;

    private Weigher<URL, CachedFile> weigher;
    private RemovalListener<URL, CachedFile> asyncRemovalListener;
    private RemoteFileLoader defaultLoader;

    private LoadingCache<URL, CachedFile> urlToFileCache;

    private CacheLoadEventListener cacheLoadEventListener;

    /**
     * Creates a new local cache whose physical storage is within the
     * specified parent directory.  The cache uses a Least Recently Used
     * algorithm to cull files from the physical directory once the
     * specified capacity (in kilobytes) is reached.
     *
     * @param  cacheParentDirectory  parent directory for the physical cache.
     *
     * @param  kilobyteCapacity      number of kilobytes to allow in the
     *                               physical cache before removing least
     *                               recently used files.
     *
     * @param  webDavClient          client for issuing WebDAV requests.
     *
     * @param  cacheLoadEventListener  listener for cache load events
     *                                 (or null if not needed).
     *
     * @throws IllegalStateException
     *   if any errors occur while constructing a cache tied to the file system.
     */
    public LocalFileCache(File cacheParentDirectory,
                          long kilobyteCapacity,
                          WebDavClient webDavClient,
                          CacheLoadEventListener cacheLoadEventListener)
            throws IllegalStateException {

        this.rootDirectory = createAndValidateDirectoryAsNeeded(cacheParentDirectory,
                                                                CACHE_DIRECTORY_NAME);
        this.activeDirectory = createAndValidateDirectoryAsNeeded(this.rootDirectory,
                                                                  ACTIVE_DIRECTORY_NAME);
        this.tempDirectory = createAndValidateDirectoryAsNeeded(this.rootDirectory,
                                                                TEMP_DIRECTORY_NAME);

        if (kilobyteCapacity < 1) {
            this.kilobyteCapacity = 1;
        } else {
            this.kilobyteCapacity = kilobyteCapacity;
        }

        this.webDavClient = webDavClient;
        this.cacheLoadEventListener = cacheLoadEventListener;

        // separate thread pool for async addition of files to the cache
        this.asyncLoadService = Executors.newFixedThreadPool(4);

        this.weigher = new Weigher<URL, CachedFile>() {

            @Override
            public int weigh(URL key,
                             CachedFile value) {

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

        // separate thread pool for removing files that expire from the cache
        final ExecutorService removalService = Executors.newFixedThreadPool(4);

        final RemovalListener<URL, CachedFile> removalListener =
                new RemovalListener<URL, CachedFile>() {
                    @Override
                    public void onRemoval(RemovalNotification<URL, CachedFile> removal) {
                        final CachedFile cachedFile = removal.getValue();
                        if (cachedFile != null) {
                            cachedFile.remove(getActiveDirectory());
                        }
                    }
                };

        this.asyncRemovalListener =
                RemovalListeners.asynchronous(removalListener, removalService);

        final LocalFileCache thisCache = this;
        this.defaultLoader = new RemoteFileLoader() {
            @Override
            public LocalFileCache getCache() {
                return thisCache;
            }
        };

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

        LOG.info("<init>: exit");
    }

    /**
     * @return the root directory for this cache.
     */
    public File getRootDirectory() {
        return rootDirectory;
    }

    /**
     * @return the directory where cached files can be loaded
     *         before they are ready to be served from the cache.
     */
    public File getTempDirectory() {
        return tempDirectory;
    }

    /**
     * @return the directory that contains all locally cached files
     *         that are ready to be served.
     */
    public File getActiveDirectory() {
        return activeDirectory;
    }

    /**
     * @return the number of files currently in the cache.
     */
    public long getNumberOfFiles() {
        return urlToFileCache.size();
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
        final Map<URL, CachedFile> internalCacheMap = urlToFileCache.asMap();
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
     * @return the client used to issue WebDAV requests for this cache.
     */
    public WebDavClient getWebDavClient() {
        return webDavClient;
    }

    /**
     * Looks for the specified resource in the cache and returns the
     * corresponding local file copy.
     * If the resource is not in the cache, it is retrieved/copied
     * (on the current thread of execution) and is added to the
     * cache before being returned.
     *
     * @param  remoteFileUrl  remote URL for the file.
     *
     * @return the local cached instance of the specified remote file.
     *
     * @throws FileNotCacheableException
     *   if the file cannot be cached locally.
     */
    public File getFile(URL remoteFileUrl)
            throws FileNotCacheableException {
        return getFile(remoteFileUrl, false);
    }

    /**
     * Looks for the specified resource in the cache and returns the
     * corresponding local file copy.
     * If the resource is not in the cache, it is retrieved/copied
     * (on the current thread of execution) and is added to the
     * cache before being returned.
     *
     * @param  remoteFileUrl  remote URL for the file.
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
    public File getFile(URL remoteFileUrl,
                        boolean forceRefresh)
            throws FileNotCacheableException {

        File localFile;

        try {
            if (forceRefresh) {
                urlToFileCache.invalidate(remoteFileUrl);
            }
            // get call should load file if it is not already present
            CachedFile cachedFile = urlToFileCache.get(remoteFileUrl);
            localFile = getVerifiedLocalFile(cachedFile);
        } catch (Exception e) {
            throw new FileNotCacheableException("failed to retrieve " + remoteFileUrl, e);
        }

        if (localFile == null) {
            throw new FileNotCacheableException("local cache file missing for " + remoteFileUrl);
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
     * @param  remoteFileUrl  remote URL for the file.
     *
     * @return the local or remote URL for the resource depending upon
     *         whether it has already been cached.
     */
    public URL getEffectiveUrl(URL remoteFileUrl) {

        URL effectiveUrl = remoteFileUrl;

        // get call will NOT load file if it is missing
        CachedFile cachedFile = urlToFileCache.getIfPresent(remoteFileUrl);
        File localFile = getVerifiedLocalFile(cachedFile);

        if (localFile == null) {

            final URL asyncRetrievalUrl = remoteFileUrl;
            asyncLoadService.submit(new Callable<File>() {
                @Override
                public File call() throws Exception {
                    return getFile(asyncRetrievalUrl);
                }
            });

        } else  {

            try {
                effectiveUrl = localFile.toURI().toURL();
            } catch (MalformedURLException e) {
                LOG.error("failed to derive URL for " + localFile.getAbsolutePath(), e);
            }
        }

        return effectiveUrl;
    }

    /**
     * Locally caches all files in the specified remote directory
     * and returns the corresponding local directory.
     * Any files not in the cache are retrieved/copied (on the current
     * thread of execution) before control is returned to the caller.
     *
     * @param  remoteDirectoryUrl  remote URL for the directory.
     *
     * @param  fetchFilesInAllSubDirectories  if true, all files within the
     *                                        directory and its sub-directories
     *                                        will be retrieved; otherwise
     *                                        only files in the immediate
     *                                        directory are retrieved.
     *
     * @param  forceRefresh   if true, will force removal of any existing
     *                        cached file before retrieving it again from
     *                        the remote source.
     *
     * @return the local cached instance of the specified remote directory.
     *
     * @throws FileNotCacheableException
     *   if any of the directory's files cannot be cached locally or
     *   if the directory does not exist or is empty.
     */
    public File getDirectory(URL remoteDirectoryUrl,
                             boolean fetchFilesInAllSubDirectories,
                             boolean forceRefresh)
            throws FileNotCacheableException {

        try {
            List<WebDavFile> webDavFiles;

            if (fetchFilesInAllSubDirectories) {
                webDavFiles = webDavClient.findAllInternalFiles(remoteDirectoryUrl);
            } else {
                webDavFiles = webDavClient.findImmediateInternalFiles(remoteDirectoryUrl);
            }

            for (WebDavFile webDavFile : webDavFiles) {
                getFile(webDavFile.getUrl(), forceRefresh);
            }

        } catch (Exception e) {
            throw new FileNotCacheableException("failed to retrieve " + remoteDirectoryUrl, e);
        }

        final File localDirectory = new File(activeDirectory, remoteDirectoryUrl.getPath());
        if (! localDirectory.exists()) {
            throw new FileNotCacheableException("local cache directory missing for " + remoteDirectoryUrl);
        }

        return localDirectory;
    }

    /**
     * Clears and removes all locally cached files.
     * Entries will be removed from the in-memory metadata cache immediately.
     * The locally cached files will be removed from the file system
     * asynchronously by a separate pool of threads.
     */
    public void clear() {
        LOG.info("clear: entry, scheduling removal of {} files from cache", urlToFileCache.size());
        urlToFileCache.invalidateAll();
    }

    @Override
    public String toString() {
        return "LocalFileCache{rootDirectory=" + rootDirectory +
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
    private File createAndValidateDirectoryAsNeeded(File parent,
                                                    String name)
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

        this.urlToFileCache =
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
            if (urlToFileCache.getIfPresent(cachedFile.getUrl()) == null) {
                urlToFileCache.put(cachedFile.getUrl(), cachedFile);
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
            if (! localFile.exists()) {
                urlToFileCache.invalidate(cachedFile.getUrl());
                localFile = null;
            }
        }
        return localFile;
    }

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileCache.class);

    private static final String CACHE_DIRECTORY_NAME = ".jacs-file-cache";
    private static final String ACTIVE_DIRECTORY_NAME = "active";
    private static final String TEMP_DIRECTORY_NAME = "temp";

}
