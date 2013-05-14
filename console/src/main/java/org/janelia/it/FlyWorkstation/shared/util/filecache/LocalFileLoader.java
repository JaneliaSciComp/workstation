package org.janelia.it.FlyWorkstation.shared.util.filecache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
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
    private WebDavClient webDavClient;
    private String standardDerivationBasePath;

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
     * @param  webDavClient                           the session WebDAV client (for repairing
     *                                                cache inconsistencies).
     */
    public LocalFileLoader(File activeDirectory,
                           boolean removeInvalidFilesAndEmptyDirectories,
                           WebDavClient webDavClient) {
        this.activeDirectory = activeDirectory;
        this.removeInvalidFilesAndEmptyDirectories = removeInvalidFilesAndEmptyDirectories;
        this.webDavClient = webDavClient;
        this.locallyCachedFiles = new ArrayList<CachedFile>(1024);

        StringBuilder sb = new StringBuilder(128);
        final String activeDirectoryPath = activeDirectory.getAbsolutePath();
        if (activeDirectoryPath.endsWith("/")) {
            sb.append(activeDirectoryPath.substring(0, activeDirectoryPath.length() - 1));
        } else {
            sb.append(activeDirectoryPath);
        }
        String relativeRootPath = "/";
        try {
            URL rootUrl = webDavClient.getWebDavUrl("/");
            relativeRootPath = rootUrl.getPath();
        } catch (MalformedURLException e) {
            LOG.warn("failed to derive root URL", e);
        }
        sb.append(relativeRootPath);

        this.standardDerivationBasePath = sb.toString();
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
                repairOrphanFiles(unregisteredChildren);
            }

            if (removeInvalidFilesAndEmptyDirectories) {
                removeDirectoryIfEmpty(file);
            }

        } else if ((file.canRead() && CachedFile.isMetaFile(file))) {
            registerMetaFile(file, unregisteredSiblings);
        }
    }

    private void repairOrphanFiles(List<File> unregisteredChildren) {
        CachedFile rebuiltMetaFile;
        for (File unregisteredChild : unregisteredChildren) {
            if (unregisteredChild.isFile()) {

                rebuiltMetaFile = rebuildMetaFile(unregisteredChild, true);

                if (rebuiltMetaFile == null) {

                    if (removeInvalidFilesAndEmptyDirectories) {
                        if (unregisteredChild.delete()) {
                            LOG.info("repairOrphanFiles: removed unregistered file " +
                                     unregisteredChild.getAbsolutePath());
                        } else {
                            LOG.warn("repairOrphanFiles: failed to remove unregistered file " +
                                     unregisteredChild.getAbsolutePath());
                        }
                    }

                } else {
                    locallyCachedFiles.add(rebuiltMetaFile);
                }

            }
        }
    }

    private void removeDirectoryIfEmpty(File directory) {
        final File[] children = directory.listFiles();
        if ((children == null) || (children.length == 0)) {
            if (! directory.delete()) {
                LOG.warn("removeDirectoryIfEmpty: failed to remove empty directory " +
                         directory.getAbsolutePath());
            }
        }
    }

    private void registerMetaFile(File metaFile,
                                  List<File> unregisteredSiblings) {

        // remove meta file from the unregistered list regardless of
        // what happens during load
        unregisteredSiblings.remove(metaFile);

        CachedFile cachedFile = CachedFile.loadPreviouslyCachedFile(metaFile);

        if (cachedFile == null) {
            cachedFile = repairMetaFile(metaFile);
        }

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

                LOG.warn("registerMetaFile: removing meta data loaded from " +
                         metaFile.getAbsolutePath() +
                         " because it identifies missing local file " + localFilePath);

                if (! metaFile.delete()) {
                    LOG.warn("registerMetaFile: failed to remove problem meta-file " +
                             metaFile.getAbsolutePath());
                }

            }

        }
    }

    private CachedFile repairMetaFile(File metaFile) {

        CachedFile cachedFile = null;

        if (metaFile.delete()) {

            final File localFile = CachedFile.getLocalFileBasedUponMetaFileName(metaFile);

            if (localFile.exists()) {
                cachedFile = rebuildMetaFile(localFile, false);
            } else {
                LOG.warn("repairMetaFile: local file " + localFile.getAbsolutePath() +
                         " missing for problem meta-file " + metaFile.getAbsolutePath() +
                         ", meta-file has simply been removed");
            }

        } else {
            LOG.warn("repairMetaFile: failed to remove problem meta-file " +
                     metaFile.getAbsolutePath());
        }

        return cachedFile;
    }

    private CachedFile rebuildMetaFile(File localFile,
                                       boolean confirmExistenceOnRemoteServer) {

        CachedFile cachedFile = null;

        final String localPath = localFile.getAbsolutePath();
        if (localPath.length() > standardDerivationBasePath.length() &&
            localPath.startsWith(standardDerivationBasePath)) {
            final String remotePath = localPath.substring(standardDerivationBasePath.length() - 1);
            URL url;
            try {
                url = webDavClient.getWebDavUrl(remotePath);
                cachedFile = new CachedFile(new WebDavFile(url, localFile), localFile);
                if (confirmExistenceOnRemoteServer && (! webDavClient.isAvailable(url))) {
                    cachedFile = null;
                    LOG.info("rebuildMetaFile: skipping creation of meta-file for " +
                             localFile.getAbsolutePath() + " because " + url + " cannot be found");
                } else {
                    cachedFile.saveMetadata();
                    LOG.info("rebuildMetaFile: saved " +
                             cachedFile.getMetaFile().getAbsolutePath());
                }
            } catch (Exception e) {
                cachedFile = null;
                LOG.warn("rebuildMetaFile: failed to create meta-file for " +
                         localFile.getAbsolutePath(), e);
            }
        } else {
            LOG.warn("rebuildMetaFile: cannot derive remote path for " +
                     localFile.getAbsolutePath());
        }

        return cachedFile;
    }

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileLoader.class);
}
