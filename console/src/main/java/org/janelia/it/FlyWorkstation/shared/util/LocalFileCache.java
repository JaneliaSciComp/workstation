package org.janelia.it.FlyWorkstation.shared.util;

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
import java.util.concurrent.ConcurrentHashMap;
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
    private int rootWithTimestampLength;
    private int kilobyteCapacity;

    private ConcurrentLinkedHashMap<String, CachedFile>
            relativePathToCachedFileMap;
    private ConcurrentHashMap<String, CachedFileBuilder>
            relativePathToBuilderMap;

    private ExecutorService removalServices;

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
                }
                return (int) kiloBytes;
            }
        };

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
     * @param  remoteFileUrl  remote URL for the file.
     *
     * @return the local cached instance of the specified remote file.
     *
     * @throws FileNotCacheableException
     *   if the file cannot be cached locally.
     */
    public File get(URL remoteFileUrl)
            throws FileNotCacheableException {

        final String relativePath =
                CachedFile.getNormalizedPath(remoteFileUrl.getPath());
        CachedFile cachedFile = relativePathToCachedFileMap.get(relativePath);
        if (cachedFile == null) {
            CachedFileBuilder builder = getBuilder(remoteFileUrl,
                                                   relativePath);
            try {
                cachedFile = builder.build();
            } catch (IllegalStateException e) {
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
    private void loadCacheFromFilesystem() {

        File[] children = rootDirectory.listFiles();
        if (children != null) {
            // only register files in retrieval timestamp directories under root
            for (File child : children) {
                if (child.isDirectory()) {
                    registerFileInCache(child);
                }
            }
        }

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
     * Internal class to facilitate synchronizing retrieval of a
     * specific remote file without delaying retrieval of other files.
     */
    private class CachedFileBuilder {
        private URL remoteFileUrl;
        private String relativePath;

        private CachedFileBuilder(URL remoteFileUrl,
                                  String relativePath) {
            this.remoteFileUrl = remoteFileUrl;
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
        private synchronized CachedFile build()
                throws IllegalStateException {

            CachedFile cachedFile =
                    relativePathToCachedFileMap.get(relativePath);
            if (cachedFile == null) {

                // Each cached file is stored within a nested set of directories as follows:
                //   [root cache directory]/[retrieval time directory]/[relative path]/[file]
                //
                // The retreival time directory is inserted to ensure that no race
                // condition is introduced by removing a file from the cache at the
                // same time it is being re-added to the cache.
                final File rootWithTimestampDirectory =
                        new File(rootDirectory, buildTimestampName());

                cachedFile = new CachedFile(rootWithTimestampDirectory,
                                            remoteFileUrl);

                // check for catastrophic case of file larger than entire cache
                final long kilobytes = cachedFile.getKilobytes();
                if (kilobytes > kilobyteCapacity) {
                    removalServices.submit(cachedFile.getRemovalTask());
                    relativePathToBuilderMap.remove(relativePath);
                    throw new IllegalStateException(
                            kilobytes +
                            " kilobyte file exceeds cache capacity of " +
                            kilobyteCapacity +
                            " kilobytes, scheduled removal of " +
                            cachedFile.getLocalFile().getAbsolutePath());
                }

                relativePathToCachedFileMap.put(relativePath, cachedFile);
                // remove builder now that the file is in the primary cache
                relativePathToBuilderMap.remove(relativePath);
            }
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
            "yyyyMMdd-hhmmssSSS";
    private static final int TIMESTAMP_LENGTH =
            TIMESTAMP_PATTERN.length() + 1;
    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat(TIMESTAMP_PATTERN);
}
