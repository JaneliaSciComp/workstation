package org.janelia.it.FlyWorkstation.shared.util.filecache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;

/**
 * <p>
 * This class encapsulates the information about a cached file.
 * It supports retrieval of the file from a remote server and
 * removal of the file when evicted from the cache.
 * </p>
 * <p>
 * When a remote file is retrieved, it is saved to the local file system
 * along with a companion file that contains a serialized form of this
 * object's metadata about the file. For example retrieval of the file
 * .../separate/ConsolidatedSignalMIP.png results in creation of a companion file
 * .../separate/ConsolidatedSignalMIP.png.jacs-cached-file.
 * Creation of these companion files allows the most important parts of the
 * in-memory cache (namely the remote URL and etag) to be persisted without
 * requiring centralized/synchronized management.
 * </p>
 *
 * @author Eric Trautman
 */
public class CachedFile implements Serializable {

    private WebDavFile webDavFile;
    private File localFile;
    private File metaFile;

    /**
     * Constructs a new instance.
     *
     * @param  webDavFile  the WebDAV information for the file's remote source.
     * @param  localFile   the local location for the file after retrieval.
     */
    public CachedFile(WebDavFile webDavFile,
                      File localFile) {
        this.webDavFile = webDavFile;
        this.localFile = localFile;
        if (! webDavFile.isDirectory()) {
            this.metaFile = new File(localFile.getAbsolutePath() + META_FILE_SUFFIX);
        }
    }

    /**
     * @return the source URL for this file.
     */
    public URL getUrl() {
        return webDavFile.getUrl();
    }

    /**
     * @return the local location of this file after retrieval.
     */
    public File getLocalFile() {
        return localFile;
    }

    /**
     * @return the local location of this file's serialized meta data.
     */
    public File getMetaFile() {
        return metaFile;
    }

    /**
     * @return the number of kilobytes in this file.
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
                "webDavFile=" + webDavFile +
                ", localFile=" + localFile.getAbsolutePath() +
                ", metaFile=" + metaFile.getAbsolutePath() +
                '}';
    }

    /**
     * Copies the remote file into the local cache storage.
     *
     * @param  tempFile  the temporary local location for retrieval.
     *                   The remote file is copied to this location first
     *                   before being moved to its final location to
     *                   ensure that only complete files are available
     *                   in the cache.
     *
     * @throws IllegalArgumentException
     *   if the remote file is a directory.
     *
     * @throws IllegalStateException
     *   if the copy fails for any reason.
     */
    public void loadRemoteFile(File tempFile)
            throws IllegalArgumentException, IllegalStateException {

        final URL remoteFileUrl = webDavFile.getUrl();

        if (webDavFile.isDirectory()) {
            throw new IllegalArgumentException(
                    "Requested load of directory " + remoteFileUrl +
                    ".  Only files may be requested.");
        }

        InputStream input = null;
        FileOutputStream output = null;

        try {

            input = remoteFileUrl.openStream();
            output = new FileOutputStream(tempFile);

            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while (EOF != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }

        } catch (Throwable t) {
            throw new IllegalStateException(
                    "failed to copy " + remoteFileUrl + " to " + tempFile.getAbsolutePath(), t);
        } finally {

            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOG.warn("loadRemoteFile: failed to close " + remoteFileUrl, e);
                }
            }

            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    LOG.warn("loadRemoteFile: failed to close " + tempFile.getAbsolutePath(), e);
                }
            }
        }

        createParentDirectroiesIfNeccesary(localFile);

        if (tempFile.renameTo(localFile)) {
            LOG.debug("loadRemoteFile: copied {} to {}", remoteFileUrl, localFile.getAbsolutePath());
        } else {
            if (! tempFile.delete()) {
                LOG.warn("loadRemoteFile: after move failure, failed to remove temp file {}",
                         tempFile.getAbsolutePath());
            }
            throw new IllegalStateException(
                    "failed to move " + tempFile.getAbsolutePath() +
                            " to " + localFile.getAbsolutePath());
        }

        if (metaFile != null) {
            saveMetadata();
        }
    }

    /**
     * Removes this file from the local file system and any empty
     * parent directories that are within the cache.  Any exceptions
     * that occur during removal are simply logged (and ignored).
     *
     * @param  activeRootDirectory  the root directory for all active files in
     *                              the cache.
     */
    public void remove(File activeRootDirectory) {
        removeFile(localFile);
        removeFile(metaFile);
        try {
            removeEmptyCacheParent(activeRootDirectory, localFile);
        } catch (IOException e) {
            LOG.warn("remove: failed to remove empty parent directories for " +
                     localFile.getAbsolutePath(), e);
        }
    }

    private void removeFile(File file) {
        if ((file != null) && file.isFile()) {
            try {
                if (file.delete()) {
                    LOG.debug("removeFile: removed {}", file.getAbsolutePath());
                } else {
                    LOG.warn("removeFile: failed to remove {}", file.getAbsolutePath());
                }
            } catch (Throwable t) {
                LOG.warn("removeFile: failed to remove " + file.getAbsolutePath(), t);
            }
        }
    }

    /**
     * Recursively walks up the directory structure until the active root
     * directory is found.  Any empty directories found along the way
     * are removed.  Special care is taken to make sure we only touch
     * directories that are within the cache.
     *
     * @param  activeRootDirectory     the root directory for all active files in
     *                                 the cache.

     * @param  removedFileOrDirectory  previously removed file or directory whose
     *                                 parent should be removed if empty.
     * @throws IOException
     *   if canonical paths cannot be derived.
     */
    private void removeEmptyCacheParent(File activeRootDirectory,
                                        File removedFileOrDirectory)
            throws IOException {

        File parent = removedFileOrDirectory.getParentFile();
        if ((parent != null) && parent.isDirectory()) {

            final String rootPath = activeRootDirectory.getCanonicalPath();
            final int minLength = rootPath.length();
            final String path = parent.getCanonicalPath();

            boolean logRemoval = LOG.isDebugEnabled();

            if ((path.length() > minLength) && path.startsWith(rootPath)) {
                final File[] children = parent.listFiles();
                if ((children != null) && (children.length == 0)) {
                    logRemoval = false;
                    if (parent.delete()) {
                        removeEmptyCacheParent(activeRootDirectory, parent);
                    } else {
                        LOG.warn("removeEmptyCacheParent: failed to remove {}", path);
                    }
                }
            }

            if (logRemoval) {
                LOG.debug("removeEmptyCacheParent: removed {}", removedFileOrDirectory.getAbsolutePath());
            }

        }

    }

    /**
     * Creates any missing parent directories for the specified file.
     *
     * @param  child  file whose parents directroies need to exist.
     *
     * @throws IllegalStateException
     *   if the parent directories do not exist and cannot be created.
     */
    private void createParentDirectroiesIfNeccesary(File child)
            throws IllegalStateException {

        final File parent = child.getParentFile();
        if (! parent.exists()) {
            if (! parent.mkdirs()) {
                // check again for parent existence in case another thread
                // created the directory while this thread was attempting
                // to create it
                if (! parent.exists()) {
                    throw new IllegalStateException(
                            "failed to create directory " + parent.getAbsolutePath());
                }
            }
        }
    }

    private void saveMetadata()
            throws IllegalStateException {

        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(metaFile));
            out.writeObject(this);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "failed to save cache file meta data to " + metaFile.getAbsolutePath(), e);

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOG.warn("saveMetadata: failed to close meta data file " + metaFile.getAbsolutePath(),
                             e);
                }
            }
        }
    }

    /**
     * @param  file  the file to check.
     *
     * @return true if the specified file is a cached file metadata file; otherwise false.
     */
    public static boolean isMetaFile(File file) {
        final String name = file.getName();
        return name.endsWith(META_FILE_SUFFIX);
    }

    /**
     * Parses the the specified metdata file and returns the corresponding
     * {@link CachedFile} instance.
     *
     * @param  metaFile  metdata file location.
     *
     * @return {@link CachedFile} instance parsed from the specified file.
     *
     * @throws IllegalStateException
     *   if any errors occur during parsing.
     */
    public static CachedFile loadPreviouslyCachedFile(File metaFile)
            throws IllegalStateException {

        CachedFile cachedFile;
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(metaFile));
            cachedFile = (CachedFile) in.readObject();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "failed to load cache file meta data from " + metaFile.getAbsolutePath(), e);

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.warn("loadPreviouslyCachedFile: failed to close meta data file " + metaFile.getAbsolutePath(),
                             e);
                }
            }
        }

        if (! cachedFile.localFile.exists()) {
            throw new IllegalStateException(
                    "meta data loaded from " + metaFile.getAbsolutePath() +
                    " identifies missing local file " + cachedFile.localFile.getAbsolutePath());
        }

        return cachedFile;
    }

    private static final Logger LOG = LoggerFactory.getLogger(CachedFile.class);

    private static final long ONE_KILOBYTE = 1024;

    private static final int EOF = -1;

    // Use 2Mb buffer to reduce likelihood of out of memory errors
    // when concurrent threads are loading images.
    // Most of the dynamic image files are around 1Mb.
    private static final int BUFFER_SIZE = 2 * 1024 * 1024; // 2Mb

    private static final String META_FILE_SUFFIX = ".jacs-cached-file";
}