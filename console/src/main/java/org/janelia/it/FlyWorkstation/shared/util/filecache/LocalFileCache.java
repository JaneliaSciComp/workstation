package org.janelia.it.FlyWorkstation.shared.util.filecache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.googlecode.concurrentlinkedhashmap.Weigher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class manages a local file cache with a defined physical
 * storage capacity.  It is designed to support fast concurrent access.
 *
 * @author Eric Trautman
 */
public class LocalFileCache {

    private File rootDirectory;
    private int rootWithTimestampLength;
    private int kilobyteCapacity;

    private ConcurrentLinkedHashMap<String, CachedFile>
            relativePathToCachedFileMap;
    private ConcurrentHashMap<String, CachedFileBuilder>
            relativePathToBuilderMap;

    private int maxNumberOfDirectoryLoadThreads;
    private ExecutorService removalServices;

    private boolean isLoadFromDiskComplete;

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
     * @throws IllegalStateException
     *   if any errors occur while constructing a cache tied to the file system.
     */
    public LocalFileCache(File cacheParentDirectory,
                          int kilobyteCapacity)
            throws IllegalStateException {

        File canonicalParent;
        try {
            canonicalParent = cacheParentDirectory.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "failed to derive canonical name for cache parent directory " +
                    cacheParentDirectory.getAbsolutePath(), e);
        }

        this.rootDirectory = new File(canonicalParent, CACHE_DIRECTORY_NAME);
        if (! rootDirectory.exists()) {
            if (! rootDirectory.mkdirs()) {
                throw new IllegalStateException(
                        "cannot create root cache directory " +
                        rootDirectory.getAbsolutePath());
            }
        }

        if (kilobyteCapacity < 1) {
            this.kilobyteCapacity = 1;
        } else {
            this.kilobyteCapacity = kilobyteCapacity;
        }

        final Weigher<CachedFile> weigher = new Weigher<CachedFile>() {
            @Override
            public int weightOf(CachedFile value) {

                long kiloBytes = value.getKilobytes();

                // doubt we'll ever have > 2000 gigabyte file,
                // but if so it simply won't be fairly weighted
                if (kiloBytes > Integer.MAX_VALUE) {
                    LOG.warn("weightOf: truncating weight for " +
                            kiloBytes + " Kb file " + value);
                    kiloBytes = Integer.MAX_VALUE;
                } else if (kiloBytes == 0) {
                    // zero weights are not supported,
                    // so we need to set empty file weight to 1
                    kiloBytes = 1;
                }
                return (int) kiloBytes;
            }
        };

        // max number of threads to use for bulk loading
        // directories with many files into cache
        this.maxNumberOfDirectoryLoadThreads = 10;

        // separate thread pool for removing files that expire from the cache
        this.removalServices = Executors.newFixedThreadPool(4);

        final EvictionListener<String, CachedFile> listener =
                new EvictionListener<String, CachedFile>() {
                    @Override
                    public void onEviction(String relativePath,
                                           CachedFile cachedFile) {
                        removalServices.submit(cachedFile.getRemovalTask());
                    }
                };

        this.relativePathToCachedFileMap =
                new ConcurrentLinkedHashMap.Builder<String, CachedFile>()
                        .maximumWeightedCapacity(kilobyteCapacity)
                        .weigher(weigher)
                        .listener(listener)
                        .build();

        // standard concurrent hash map will suffice for the cached file
        // builders since items are removed as soon as build completes
        this.relativePathToBuilderMap =
                new ConcurrentHashMap<String, CachedFileBuilder>();

        // save static length of all retrieval time directory paths here
        // so that it does not need to be recalculated over and over
        final String rootDirectoryAbsolutePath =
                this.rootDirectory.getAbsolutePath();
        this.rootWithTimestampLength =
                rootDirectoryAbsolutePath.length() + TIMESTAMP_LENGTH;

        if (rootDirectory.canWrite()) {

            // load cache from a separate thread so that we don't
            // bog down application start up
            Thread loadThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    loadCacheFromFilesystem();
                }
            }, "local-file-cache-load-thread");
            loadThread.start();

        } else {
            throw new IllegalStateException(
                    "root cache directory " + rootDirectory.getAbsolutePath() +
                    " is not writable");
        }

        LOG.info("<init>: exit");
    }

    /**
     * @return the root directory for this cache.
     */
    public File getRootDirectory() {
        return rootDirectory;
    }

    /**
     * @return the number of files currently in the cache.
     */
    public int getNumberOfFiles() {
        return relativePathToCachedFileMap.size();
    }

    /**
     * @return the number of kilobytes currently stored in the cache.
     */
    public long getNumberOfKilobytes() {
        return relativePathToCachedFileMap.weightedSize();
    }

    /**
     * @return the maximum number of kilobytes to be maintained in this cache.
     */
    public int getKilobyteCapacity() {
        return kilobyteCapacity;
    }

    /**
     * Sets the maximum number of kilobytes to be maintained in this cache
     * and eagerly evicts files until it shrinks to the appropriate size.
     *
     * @param  kilobyteCapacity  maximum cache capacity in kilobytes.
     */
    public void setKilobyteCapacity(int kilobyteCapacity) {
        LOG.info("setKilobyteCapacity: entry, kilobyteCapacity={}", kilobyteCapacity);
        relativePathToCachedFileMap.setCapacity(kilobyteCapacity);
        this.kilobyteCapacity = kilobyteCapacity;
    }

    /**
     * @param  remoteFileUrl  remote URL for the file.
     *
     * @return the local cached instance of the specified remote file.
     *
     * @throws FileNotCacheableException
     *   if the file cannot be cached locally.
     */
    public File getFile(URL remoteFileUrl)
            throws FileNotCacheableException {

        if (! isLoadFromDiskComplete) {
            waitForLocalFilesystemLoadToComplete();
        }

        final String relativePath =
                CachedFile.getNormalizedPath(remoteFileUrl.getPath());
        CachedFile cachedFile = relativePathToCachedFileMap.get(relativePath);
        if (cachedFile == null) {
            CachedFileBuilder builder = getBuilder(remoteFileUrl,
                                                   relativePath);
            try {
                cachedFile = builder.loadFile();
            } catch (Exception e) {
                throw new FileNotCacheableException(
                        "failed to cache " + remoteFileUrl, e);
            }
        }

        final File localFile = cachedFile.getLocalFile();
        if (! localFile.exists()) {
            throw new FileNotCacheableException(
                    "local cache file missing for " + remoteFileUrl);
        }

        return localFile;
    }

    /**
     * @param  remoteDirectoryUrl  remote URL for the directory
     *                             (path must end with '/').
     * @param  webDavClient        client to use for WebDAV queries.
     *
     * @return the local cached instance of the specified remote directory.

     * @throws IllegalArgumentException
     *   if the directory path does not end with '/'.

     * @throws FileNotCacheableException
     *   if the directory cannot be cached locally.
     */
    public File getDirectory(URL remoteDirectoryUrl,
                             WebDavClient webDavClient)
            throws IllegalArgumentException, FileNotCacheableException {

        if (! isLoadFromDiskComplete) {
            waitForLocalFilesystemLoadToComplete();
        }

        String relativePath =
                CachedFile.getNormalizedPath(remoteDirectoryUrl.getPath());
        if (relativePath.charAt(relativePath.length() - 1) != '/') {
            throw new IllegalArgumentException(
                    "directory path '" + relativePath +
                    " does not end with '/'");
        }

        CachedFile cachedDirectory =
                relativePathToCachedFileMap.get(relativePath);
        if (cachedDirectory == null) {
            CachedFileBuilder builder = getBuilder(remoteDirectoryUrl,
                                                   relativePath);
            try {
                cachedDirectory = builder.loadDirectory(webDavClient);
            } catch (Exception e) {
                throw new FileNotCacheableException(
                        "failed to cache directory " + remoteDirectoryUrl, e);
            }
        }
        final File localDirectory = cachedDirectory.getLocalFile();
        if (! localDirectory.exists()) {
            throw new FileNotCacheableException(
                    "local cache directory missing for " + remoteDirectoryUrl);
        }

        return localDirectory;
    }

    /**
     * Clears and removes all locally cached files.
     * NOTE: all file removal is performed by the calling thread.
     */
    public void clear() {
        CachedFile cachedFile;
        for (String path : relativePathToCachedFileMap.keySet()) {
            cachedFile = relativePathToCachedFileMap.remove(path);
            try {
                cachedFile.getRemovalTask().call();
            } catch (Exception e) {
                LOG.error("failed to remove " + cachedFile);
            }
        }
    }

    @Override
    public String toString() {
        return "LocalFileCache{rootDirectory=" + rootDirectory +
                ", kilobyteCapacity=" + kilobyteCapacity +
                '}';
    }

    /**
     * Ensures that multiple requests for the same relative path
     * return the same {@link CachedFileBuilder} instance.
     * This ultimately allows us to serialize (and prevent duplicate
     * retrieval) for a specific file while only minimally
     * penalizing requests for other files by other threads.
     *
     * @param  remoteFileUrl  URL of file to retrieve.
     * @param  relativePath   normalized relative path for the file.
     *
     * @return the builder instance for the specified file name.
     */
    private synchronized CachedFileBuilder getBuilder(URL remoteFileUrl,
                                                      String relativePath) {

        CachedFileBuilder builder = relativePathToBuilderMap.get(relativePath);
        if (builder == null) {
            builder = new CachedFileBuilder(remoteFileUrl, relativePath);
            relativePathToBuilderMap.put(relativePath, builder);
        }
        return builder;
    }

    /**
     * Registers any existing local files in this cache.
     *
     * NOTE: After load, cache usage (ordering) will simply reflect
     *       directory traversal order.
     */
    protected void loadCacheFromFilesystem() {

        isLoadFromDiskComplete = false;

        // remove entries from map but leave actual files on disk alone
        relativePathToCachedFileMap.clear();

        File[] children = rootDirectory.listFiles();
        if (children != null) {
            // only register files in retrieval timestamp directories under root
            for (File child : children) {
                if (child.isDirectory()) {
                    registerFileInCache(child);
                }
            }
        }

        isLoadFromDiskComplete = true;

        final long usedKb = getNumberOfKilobytes();
        final long totalKb = getKilobyteCapacity();
        final int usedPercentage = (int)
                (((double) usedKb / (double) totalKb) * 100);

        LOG.info("loadCacheFromFilesystem: loaded " + this +
                ", " + usedPercentage + "% full (" + getNumberOfKilobytes() + "/" +
                getKilobyteCapacity() + " kilobytes)");
    }

    /**
     * Registers the specified file in the local cache.
     * If the file is a directory, its children are recursively registered.
     * Any empty directories discovered during traversal are removed.
     *
     * @param  file  file to register.
     */
    private void registerFileInCache(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    registerFileInCache(child);
                }
            }

            children = file.listFiles();
            if ((children == null) || (children.length == 0)) {
                if (! file.delete()) {
                    LOG.warn("registerFileInCache: failed to remove " +
                             "empty directory " + file.getAbsolutePath());
                }
            }

        } else if (file.canRead()) {
            try {
                final String absoluteFilePath = file.getAbsolutePath();
                if (absoluteFilePath.length() > rootWithTimestampLength) {
                    final String rootWithTimestampPath =
                            absoluteFilePath.substring(0, rootWithTimestampLength);
                    final File rootWithTimestampDirectory =
                            new File(rootWithTimestampPath);
                    CachedFile cachedFile =
                            new CachedFile(rootWithTimestampDirectory, file);
                    relativePathToCachedFileMap.put(
                            cachedFile.getRelativePath(),
                            cachedFile);
                } else {
                    LOG.warn("registerFileInCache: ignoring " +
                             file.getAbsolutePath());
                }
            } catch (Exception e) {
                LOG.warn("registerFileInCache: failed to load " +
                         file.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Blocks until cache load completes or until maximum wait time is reached.
     *
     * @throws FileNotCacheableException
     *   if the maximum wait time is exceeded.
     */
    private void waitForLocalFilesystemLoadToComplete()
            throws FileNotCacheableException {

        final int maxAttempts = 10;
        final long waitMilliseconds = 500;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(waitMilliseconds);
                if (isLoadFromDiskComplete) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new FileNotCacheableException(
                        "cache file system load interrupted", e);
            }
        }

        if (! isLoadFromDiskComplete) {
            final long totalWait = (maxAttempts * waitMilliseconds) / 1000;
            throw new FileNotCacheableException(
                    "cache file system load not complete after waiting " +
                    totalWait + " seconds");
        }
    }

    /**
     * <p>
     * Internal class to facilitate synchronizing retrieval of a
     * specific remote file or directory without delaying retrieval
     * of other files.
     * </p>
     * <p>
     * Each cached file/directory is stored within a nested set of
     * directories as follows:
     * <pre>
     *     [root cache directory]/[retrieval time directory]/[relative path]
     * </pre>
     * The retreival time directory is inserted to ensure that no race
     * condition is introduced by removing a file from the cache at the
     * same time it is being re-added to the cache.
     * </p>
     */
    private class CachedFileBuilder {
        private URL remoteUrl;
        private File timestampRootDirectory;
        private String relativePath;

        public CachedFileBuilder(URL remoteUrl,
                                 String relativePath) {
            this.remoteUrl = remoteUrl;
            this.timestampRootDirectory = new File(rootDirectory,
                                                   buildTimestampName());
            this.relativePath = relativePath;
        }

        /**
         * Ensures remote file is only retrieved once.
         *
         * @return cached file for this builder's remote file URL.
         *
         * @throws IllegalStateException
         *   if the file cannot be cached.
         */
        public synchronized CachedFile loadFile()
                throws IllegalStateException {

            CachedFile cachedFile;
            try {
                cachedFile = relativePathToCachedFileMap.get(relativePath);
                if (cachedFile == null) {
                    cachedFile = loadFile(relativePath, remoteUrl);
                }
            } finally {
                // make sure builder is removed after load
                relativePathToBuilderMap.remove(relativePath);
            }

            return cachedFile;
        }

        /**
         * Ensures remote directory is only retrieved once.
         *
         * @throws WebDavRetrievalException
         *   if information about the remote directory cannot be retrieved.
         *
         * @throws IllegalStateException
         *   if the directory cannot be cached.
         */
        public synchronized CachedFile loadDirectory(WebDavClient webDavClient)
                throws WebDavRetrievalException, IllegalStateException {

            CachedFile cachedDirectory;
            try {
                List<WebDavFile> webDavFileList =
                        webDavClient.findAllInternalFiles(remoteUrl);

                // TODO: apply filters (depth, pattern, ...)

                ExecutorService loadServices = null;
                if (webDavFileList.size() > maxNumberOfDirectoryLoadThreads) {
                    loadServices = Executors.newFixedThreadPool(maxNumberOfDirectoryLoadThreads);
                }

                URL fileUrl;
                String fileRelativePath;
                CachedFile cachedFile;
                for (WebDavFile webDavFile : webDavFileList) {
                    fileUrl = webDavFile.getUrl();
                    fileRelativePath = fileUrl.getPath();
                    cachedFile = relativePathToCachedFileMap.get(fileRelativePath);
                    if (cachedFile == null) {
                        if (webDavFile.getKilobytes() < kilobyteCapacity) {
                            if (loadServices == null) {
                                loadFile(fileRelativePath, fileUrl);
                            } else {
                                final URL fUrl = fileUrl;
                                final String fPath = fileRelativePath;
                                loadServices.submit(new Callable<Object>() {
                                    @Override
                                    public Object call() throws Exception {
                                        return loadFile(fPath, fUrl);
                                    }
                                });
                            }
                        } else {
                            LOG.warn(webDavFile.getKilobytes() +
                                     " kilobyte file " +
                                     fileUrl + " exceeds cache capacity of " +
                                     kilobyteCapacity + " kilobytes, skipping load");
                        }
                    } else {
                        cachedFile.moveLocalFile(timestampRootDirectory);
                    }
                }

                if (loadServices != null) {
                    LOG.info("loadDirectory: waiting for load of {}", remoteUrl);
                    try {
                        loadServices.shutdown();
                        loadServices.awaitTermination(1, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        LOG.error("loadDirectory: interrupted load of " + remoteUrl, e);
                        throw new IllegalStateException(
                                "load of " + remoteUrl + " was interrupted");
                    }
                }

                final File localDirectory =
                        new File(timestampRootDirectory + relativePath);
                try {
                    cachedDirectory = new CachedFile(timestampRootDirectory,
                                                     localDirectory);
                    relativePathToCachedFileMap.put(relativePath,
                                                    cachedDirectory);
                } catch (IOException e) {
                    throw new IllegalStateException(
                            "failed to create cache entry for " +
                            localDirectory.getAbsolutePath());
                }

            } finally {
                // make sure builder is removed after load
                relativePathToBuilderMap.remove(relativePath);
            }

            return cachedDirectory;
        }

        private CachedFile loadFile(String fileRelativePath,
                                    URL fileUrl)
                throws IllegalStateException {

            CachedFile cachedFile = new CachedFile(timestampRootDirectory,
                                                   fileUrl);

            // check for catastrophic case of file larger than entire cache
            final long kilobytes = cachedFile.getKilobytes();
            if (kilobytes > kilobyteCapacity) {
                removalServices.submit(cachedFile.getRemovalTask());
                throw new IllegalStateException(
                        kilobytes +
                        " kilobyte file exceeds cache capacity of " +
                        kilobyteCapacity +
                        " kilobytes, scheduled removal of " +
                        cachedFile.getLocalFile().getAbsolutePath());
            }

            relativePathToCachedFileMap.put(fileRelativePath, cachedFile);

            return cachedFile;
        }

    }

    /**
     * @return a new timestamp directory name based on the current time.
     */
    protected static String buildTimestampName() {
        return TIMESTAMP_FORMAT.format(new Date());
    }

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileCache.class);

    private static final String CACHE_DIRECTORY_NAME = ".jacs-file-cache";
    private static final String TIMESTAMP_PATTERN =
            "yyyyMMdd-HHmmssSSS";
    private static final int TIMESTAMP_LENGTH =
            TIMESTAMP_PATTERN.length() + 1;
    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat(TIMESTAMP_PATTERN);
}
