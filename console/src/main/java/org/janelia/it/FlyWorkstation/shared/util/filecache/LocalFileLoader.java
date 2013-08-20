package org.janelia.it.FlyWorkstation.shared.util.filecache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * This class traverses the local filesystem to identify files that were cached
 * by previous runs.  See {@link CachedFile} for details on how cached files and
 * their metadata are persisted to the local file store.
 *
 * @author Eric Trautman
 */
public class LocalFileLoader {

    private File activeDirectory;

    private List<CachedFile> locallyCachedFiles;
    private Set<File> unregisteredFiles;

    /**
     * Constructs a local file loader instance.
     *
     * The removeInvalidFilesAndEmptyDirectories parameter is provided to allow
     * callers to opt-in to the somewhat risky task of automatic deletion/clean-up
     * of the the specified active directory.  Automatic deletion/clean-up is fine
     * as long as you are sure the specified active directory only contains cache
     * files.  However if the wrong directory (e.g. the user home directory)
     * was acidentally provided, enabling this parameter would be a big problem.
     *
     * @param  activeDirectory                        the active directory for the local file cache.
     */
    public LocalFileLoader(File activeDirectory) {
        this.activeDirectory = activeDirectory;
        this.locallyCachedFiles = new ArrayList<CachedFile>(1024);
        this.unregisteredFiles = new HashSet<File>(1024);
    }

    /**
     * @return set of files in the cache that could not be registered.
     */
    public Set<File> getUnregisteredFiles() {
        return unregisteredFiles;
    }

    /**
     * Locates all cached files stored within the cache active directory.
     *
     * All empty sub-directories detected during location are removed.
     *
     * @return the list of locally cached files.
     */
    public List<CachedFile> locateCachedFiles() {

        locallyCachedFiles.clear();
        unregisteredFiles.clear();

        File[] children = activeDirectory.listFiles();
        if (children != null) {
            // only register files under root
            for (File child : children) {
                List<File> unregisteredChildren = new ArrayList<File>(Arrays.asList(children));
                if (child.isDirectory()) {
                    registerFile(child, unregisteredChildren);
                }
            }
        }

        if (unregisteredFiles.size() > 0) {

            for (File unrgisteredFile : unregisteredFiles) {
                if (unrgisteredFile.delete()) {
                    LOG.info("removed unregistered cache file {}", unrgisteredFile.getAbsolutePath());
                } else {
                    LOG.warn("failed to remove unregistered cache file {}", unrgisteredFile.getAbsolutePath());
                }
            }

        } else {
            LOG.info("no unregistered files found in local cache");
        }

        return locallyCachedFiles;
    }

    /**
     * Adds the specified file to the list of local cached files.
     *
     * If the file is a directory, its children are recursively checked.
     * If the removeInvalidFilesAndEmptyDirectories attribute is set,
     * any invalid files and empty directories discovered during traversal
     * are also removed.
     *
     * @param  file  file to register.
     */
    private void registerFile(File file,
                              List<File> unregisteredSiblings) {

        if (file.isDirectory()) {

            File[] children = file.listFiles();
            if (children != null) {
                List<File> unregisteredChildren = new ArrayList<File>(Arrays.asList(children));
                for (File child : children) {
                    registerFile(child, unregisteredChildren);
                }
                for (File unregisteredChild : unregisteredChildren) {
                    // only add files to official unregistered list
                    if (unregisteredChild.isFile()) {
                        unregisteredFiles.add(unregisteredChild);
                    }
                }
            }

            removeDirectoryIfEmpty(file);

        } else if ((file.canRead() && CachedFile.isMetaFile(file))) {

            registerMetaFile(file, unregisteredSiblings);

        }
    }

    private void removeDirectoryIfEmpty(File directory) {
        if (directory.isDirectory()) {
            final File[] children = directory.listFiles();
            if ((children == null) || (children.length == 0)) {
                if (directory.delete()) {
                    LOG.info("removeDirectoryIfEmpty: removed {}", directory.getAbsolutePath());
                } else {
                    LOG.warn("removeDirectoryIfEmpty: failed to remove {}",
                             directory.getAbsolutePath());
                }
            }
        }
    }

    private void registerMetaFile(File metaFile,
                                  List<File> unregisteredSiblings) {

        // remove meta file from the unregistered list regardless of
        // what happens during load
        unregisteredSiblings.remove(metaFile);

        CachedFile cachedFile = CachedFile.loadPreviouslyCachedFile(metaFile);

        if (cachedFile != null) {

            final File localFile = cachedFile.getLocalFile();

            if ((localFile != null) && localFile.exists()) {

                locallyCachedFiles.add(cachedFile);
                unregisteredSiblings.remove(localFile);

            } else {

                String localFilePath = null;
                if (localFile != null) {
                    localFilePath = localFile.getAbsolutePath();
                }

                final String msg = "meta-file " + metaFile.getAbsolutePath() +
                                   " because it identifies missing local file " + localFilePath;
                if (metaFile.delete()) {
                    LOG.info("registerMetaFile: removed {}", msg);
                } else {
                    LOG.error("registerMetaFile: failed to remove {}", msg);
                }

            }

        } else {

            final String msg = "meta-file " + metaFile.getAbsolutePath() + " because it cannot be parsed";
            if (metaFile.delete()) {
                LOG.info("registerMetaFile: removed {}", msg);
            } else {
                LOG.error("registerMetaFile: failed to remove {}", msg);
            }

        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileLoader.class);
}
