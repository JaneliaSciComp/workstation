package org.janelia.it.FlyWorkstation.shared.util.filecache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class traverses the local filesystem to identify files that were cached
 * by previous runs.  See {@link CachedFile} for details on how cached files and
 * their metadata are persisted to the local file store.
 *
 * @author Eric Trautman
 */
public class LocalFileLoader {

    private File activeDirectory;
    private boolean removeInvalidFilesAndEmptyDirectories;

    private List<CachedFile> locallyCachedFiles;

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
     * @param  removeInvalidFilesAndEmptyDirectories  indicates whether invalid files (e.g. files
     *                                                with missing companions) and empty
     *                                                sub-directories should be removed during
     *                                                location.
     */
    public LocalFileLoader(File activeDirectory,
                           boolean removeInvalidFilesAndEmptyDirectories) {
        this.activeDirectory = activeDirectory;
        this.removeInvalidFilesAndEmptyDirectories = removeInvalidFilesAndEmptyDirectories;
        this.locallyCachedFiles = new ArrayList<CachedFile>(1024);
    }

    /**
     * Locates all cached files stored within the cache active directory.
     *
     * If the {@link #removeInvalidFilesAndEmptyDirectories} attribute has been enabled,
     * invalid files and empty directries will be removed during location.
     *
     * @return the list of locally cached files.
     */
    public List<CachedFile> locateCachedFiles() {

        locallyCachedFiles.clear();

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

                if (removeInvalidFilesAndEmptyDirectories) {
                    for (File unregisteredChild : unregisteredChildren) {
                        if (unregisteredChild.isFile()) {
                            if (! unregisteredChild.delete()) {
                                LOG.warn("registerFile: failed to remove unregistered file " +
                                         unregisteredChild.getAbsolutePath());
                            }
                        }
                    }
                }
            }

            if (removeInvalidFilesAndEmptyDirectories) {
                // check directory files again since some may be been removed
                children = file.listFiles();
                if ((children == null) || (children.length == 0)) {
                    if (! file.delete()) {
                        LOG.warn("registerFile: failed to remove empty directory " +
                                 file.getAbsolutePath());
                    }
                }
            }

        } else if ((file.canRead() && CachedFile.isMetaFile(file))) {

            try {
                CachedFile cachedFile = CachedFile.loadPreviouslyCachedFile(file);
                locallyCachedFiles.add(cachedFile);
                unregisteredSiblings.remove(file);
                unregisteredSiblings.remove(cachedFile.getLocalFile());
            } catch (Exception e) {
                LOG.warn("registerFile: failed to load " + file.getAbsolutePath() + ", removing file", e);
                try {
                    if (file.delete()) {
                        unregisteredSiblings.remove(file);
                    } else {
                        LOG.warn("registerFile: failed to remove problem meta-file " + file.getAbsolutePath());
                    }
                } catch (Exception e2) {
                    LOG.warn("registerFile: failed to remove problem meta-file " + file.getAbsolutePath(), e2);
                }
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileLoader.class);
}
