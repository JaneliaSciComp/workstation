package org.janelia.it.FlyWorkstation.shared.util;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

/**
 * <p>
 * This class encapsulates the information about a cached file.
 * It ensures that the file is only retrieved once when concurrent requests
 * are made and supports removal of the file when evicted from the cache.
 * </p>
 * <p>
 * Each cached file is stored within a nested set of directories as follows:
 * <pre>
 *     [root cache directory]/[retrieval time directory]/[relative path]/[file]
 * </pre>
 * <p>
 * The retreival time directory is inserted to ensure that no race
 * condition is introduced by removing a file from the cache at the
 * same time it is being re-added to the cache.
 * </p>
 * <p>
 * The relative path directory is inserted to allow the cache to be
 * reloaded from local disk at start-up.
 * </p>
 *
 * @author Eric Trautman
 */
public class CachedFile {

    /**
     * @return a normalized version of the specified path
     *         (replaces all backward slashes with forward slashes).
     */
    public static String getNormalizedPath(String path) {
        return(path.replace('\\','/'));
    }

    /**
     * @return a new timestamp directory name based on the current time.
     */
    public static String buildTimestampName() {
        return TIMESTAMP_FORMAT.format(new Date());
    }

    private File cacheRootDirectory;
    private URL remoteFileUrl;
    private String relativePath;
    private File localFile;

    /**
     * Constructs a cached file instance that needs to be retrieved
     * from a remote location.
     *
     * @param  remoteFileUrl  identifies location of the remote file.
     *
     * @throws IllegalStateException
     *   if the file cannot be cached locally.
     */
    public CachedFile(File cacheRootDirectory,
                      URL remoteFileUrl)
            throws IllegalStateException {

        this.cacheRootDirectory = cacheRootDirectory;
        this.remoteFileUrl = remoteFileUrl;
        this.relativePath = getNormalizedPath(remoteFileUrl.getPath());
        final File timestampDirectory = new File(cacheRootDirectory,
                                                 buildTimestampName());
        this.localFile = new File(timestampDirectory,
                                  this.relativePath);

        if (! this.localFile.exists()) {
            copyRemoteFile();
        }

    }

    /**
     * Constructs a cached file instance from a file that has previously
     * been cached.
     *
     * @param  cacheRootDirectory  the root directory that contains all
     *                             cached files.
     * @param  localFile           the pre-existing locally cached file.
     *
     * @throws IOException
     *   if an error occurs deriving canonical paths for the specified files.
     *
     * @throws IllegalArgumentException
     *   if the cache root directory does not contain the specified file.
     */
    public CachedFile(File cacheRootDirectory,
                      File localFile)
            throws IOException, IllegalArgumentException {

        this.cacheRootDirectory = cacheRootDirectory;
        this.localFile = localFile;
        this.relativePath = null;
        this.remoteFileUrl = null;

        final String rootPath = cacheRootDirectory.getCanonicalPath();
        final String localPath = localFile.getCanonicalPath();
        if (localPath.startsWith(rootPath)) {
            final int rootWithTimestampLength =
                    rootPath.length() + TIMESTAMP_LENGTH;
            if (localPath.length() > rootWithTimestampLength) {
                final String relativePath =
                        localPath.substring(rootWithTimestampLength);
                this.relativePath = getNormalizedPath(relativePath);
            }
        }

        if (this.relativePath == null) {
            throw new IllegalArgumentException(
                    "cache root directory " + rootPath +
                    " does not contain local file " + localPath);
        }

        LOG.info("<init>: loaded " + localFile.getAbsolutePath());
    }

    /**
     * @return the normalized relative path for this file.
     */
    public String getRelativePath() {
        return relativePath;
    }

    /**
     * @return the location of this locally cached file
     *         (NOTE: physical file may not exist).
     */
    public File getLocalFile() {
        return localFile;
    }

    /**
     * @return the number of kilobytes in the locally cached file.
     */
    public long getKilobytes() {
        long kilobytes = 0;
        if (localFile.exists()) {
            final long len = localFile.length();
            kilobytes = len / ONE_KILOBYTE;
            if ((len % ONE_KILOBYTE) > 0) {
                kilobytes++; // round up to nearest kb
            }
        }
        return kilobytes;
    }

    @Override
    public String toString() {
        return "CachedFile{" +
                "localFile=" + localFile.getAbsolutePath() +
                ", remoteFileUrl=" + remoteFileUrl +
                '}';
    }

    /**
     * @return a task that removes this file from the local file system.
     */
    public Callable<Void> getRemovalTask() {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                removeLocalFile();
                return null;
            }
        };
    }

    /**
     * Copies the remote file into the local cache storage.
     *
     * @throws IllegalStateException
     *   if the copy fails for any reason.
     */
    private void copyRemoteFile() throws IllegalStateException {

        final File parent = localFile.getParentFile();
        if (! parent.mkdirs()) {
            throw new IllegalStateException(
                    "failed to create directory " +
                            parent.getAbsolutePath());
        }

        InputStream input = null;
        FileOutputStream output = null;

        try {

            input = remoteFileUrl.openStream();
            output = new FileOutputStream(localFile);

            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while (EOF != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }

            LOG.info("copyRemoteFile: copied " + remoteFileUrl + " to " +
                     localFile.getAbsolutePath());

        } catch (Throwable t) {
            throw new IllegalStateException(
                    "failed to copy " + remoteFileUrl +
                    " to " + localFile.getAbsolutePath(), t);
        } finally {

            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOG.warn("copyRemoteFile: failed to close " +
                             remoteFileUrl, e);
                }
            }

            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    LOG.warn("copyRemoteFile: failed to close " +
                             localFile.getAbsolutePath(), e);
                }
            }
        }

    }

    /**
     * Removes this file from the local file system and any empty
     * parent directories that are within the cache.  Any exceptions
     * that occur during removal are simply logged (and ignored).
     */
    private void removeLocalFile() {
        try {
            if (localFile.isFile()) {
                if (localFile.delete()) {
                    LOG.info("removeLocalFile: removed " +
                             localFile.getAbsolutePath());
                    try {
                        removeEmptyCacheParent(localFile);
                    } catch (IOException e) {
                        LOG.warn("removeLocalFile: failed to remove empty " +
                                 "parent directories for " +
                                 localFile.getAbsolutePath(), e);
                    }
                } else {
                    LOG.warn("removeLocalFile: failed to remove " +
                             localFile.getAbsolutePath());
                }
            }
        } catch (Throwable t) {
            LOG.warn("removeLocalFile: failed to remove " +
                     localFile.getAbsolutePath(), t);
        }
    }

    /**
     * Recursively walks up the directory structure until the cache root
     * directory is found.  Any empty directories found along the way
     * are removed.  Special care is taken to make sure we only touch
     * directories that are within the cache.
     *
     * @param  removedFileOrDirectory  previously removed file or
     *                                 directory whose parent should
     *                                 be removed if empty.
     * @throws IOException
     *   if canonical paths cannot be derived.
     */
    private void removeEmptyCacheParent(File removedFileOrDirectory)
            throws IOException {

        File parent = removedFileOrDirectory.getParentFile();
        if ((parent != null) && parent.isDirectory()) {

            final String rootPath = cacheRootDirectory.getCanonicalPath();
            final int minLength = rootPath.length() + TIMESTAMP_LENGTH;
            final String path = parent.getCanonicalPath();

            boolean logRemoval = LOG.isInfoEnabled();

            if ((path.length() >= minLength) && path.startsWith(rootPath)) {
                final File[] children = parent.listFiles();
                if ((children != null) && (children.length == 0)) {
                    logRemoval = false;
                    if (parent.delete()) {
                        removeEmptyCacheParent(parent);
                    } else {
                        LOG.warn("removeEmptyCacheParent: failed to remove " +
                                 path);
                    }
                }
            }

            if (logRemoval) {
                LOG.info("removeEmptyCacheParent: removed " +
                         removedFileOrDirectory.getAbsolutePath());
            }

        }

    }

    private static final Logger LOG = Logger.getLogger(CachedFile.class);

    private static final long ONE_KILOBYTE = 1024;

    private static final int EOF = -1;

    // Use 2Mb buffer to reduce likelihood of out of memory errors
    // when concurrent threads are loading images.
    // Most of the dynamic image files are around 1Mb.
    private static final int BUFFER_SIZE = 2 * 1024 * 1024; // 2Mb

    private static final String TIMESTAMP_PATTERN =
            "yyyyMMdd-hhmmssSSS";
    private static final int TIMESTAMP_LENGTH =
            TIMESTAMP_PATTERN.length() + 1;
    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat(TIMESTAMP_PATTERN);
}