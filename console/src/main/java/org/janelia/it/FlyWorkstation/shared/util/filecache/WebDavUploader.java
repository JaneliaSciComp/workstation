package org.janelia.it.FlyWorkstation.shared.util.filecache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Utility to upload files to the server via WebDAV.
 *
 * @author Eric Trautman
 */
public class WebDavUploader {

    private WebDavClient client;
    private String rootRemoteUploadPath;

    /**
     * Constructs an uploader using the default jacs root upload path.
     *
     * @param  client  WebDAV client for the current session
     *                 (see {@link org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr#getWebDavClient()}).
     */
    public WebDavUploader(WebDavClient client) {
        this(client, JACS_ROOT_UPLOAD_PATH);
    }

    /**
     * Constructs an uploader using the specified root upload path.
     *
     * @param  client  WebDAV client for the current session
     *                 (see {@link org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr#getWebDavClient()}).
     *
     * @param  rootRemoteUploadPath root path on server for all uploaded files.
     */
    public WebDavUploader(WebDavClient client,
                          String rootRemoteUploadPath) {
        this.client = client;
        this.rootRemoteUploadPath = rootRemoteUploadPath;
    }

    /**
     * Uplaods the specified file to the server.
     *
     * @param  file  file to upload.
     *
     * @return the server path for the parent directory of the uploaded file.
     *
     * @throws WebDavException
     *   if the file cannot be uploaded.
     */
    public String uploadFile(File file) throws WebDavException {
        final String remoteUploadDirectoryPath =
                client.getUniqueUploadDirectoryPath(rootRemoteUploadPath);
        final String remoteFilePath = remoteUploadDirectoryPath + file.getName();
        createDirectory(remoteUploadDirectoryPath);
        uploadFile(remoteFilePath, file);

        LOG.info("uploaded {} to {}", file.getAbsolutePath(), remoteFilePath);

        return remoteUploadDirectoryPath;
    }

    /**
     * Uploads the specified files to the server.
     *
     * @param  fileList            list of local files to upload.
     *
     * @param  localRootDirectory  a common parent of all listed files that is used to determine
     *                             the relative path for each file on the server.
     *                             For example given files /a/b/f1.txt and /a/b/c/f2.txt
     *                             and a localRootDirectory of /a, the server paths would be:
     *                             [upload-dir]/b/f1.txt and [upload-dir]/b/c/f2.txt.
     *                             This parameter is optional.
     *                             Specifiy null if the relative paths are not important AND
     *                             all file names are unique.
     *
     * @return the server path for the parent directory of all uploaded files.
     *
     * @throws IllegalArgumentException
     *   if the server paths cannot be derived or are not unique.
     *
     * @throws WebDavException
     *   if the files cannot be uploaded.
     */
    public String uploadFiles(List<File> fileList,
                              File localRootDirectory)
            throws IllegalArgumentException, WebDavException {

        final String remoteUploadDirectoryPath =
                client.getUniqueUploadDirectoryPath(rootRemoteUploadPath);

        createDirectory(remoteUploadDirectoryPath);

        Map<String, File> remotePathToFileMap = new HashMap<String, File>(fileList.size());

        String path;
        if (localRootDirectory == null) {

            for (File file : fileList) {
                path = remoteUploadDirectoryPath + file.getName();
                if (remotePathToFileMap.containsKey(path)) {
                    throw new IllegalArgumentException("multiple files share the name '" +
                                                       file.getName() + "'");
                }
                remotePathToFileMap.put(path, file);
            }

        } else if (localRootDirectory.exists()) {

            List<String> orderedDirectoryPaths = new ArrayList<String>(fileList.size());

            List<String> localFilePaths = new ArrayList<String>(fileList.size());

            try {
            for (File file : fileList) {
                if (file.isFile()) {
                    localFilePaths.add(file.getCanonicalPath());
                }
            }

            derivePaths(localRootDirectory.getCanonicalPath() + File.separator,
                        localFilePaths,
                        remoteUploadDirectoryPath,
                        orderedDirectoryPaths,
                        remotePathToFileMap);

            } catch (IOException e) {
                throw new IllegalArgumentException("canonical path could not be derived", e);
            }

            if (orderedDirectoryPaths.size() > 0) {
                for (String dirPath : orderedDirectoryPaths) {
                    createDirectory(dirPath);
                }

                LOG.info("created {} directories under {}",
                         orderedDirectoryPaths.size(), remoteUploadDirectoryPath);
            }

        } else {

            throw new IllegalArgumentException(
                    "specified local root directory " + localRootDirectory.getAbsolutePath() +
                    " does not exist");
        }

        uploadFiles(remotePathToFileMap);

        LOG.info("uploaded {} files to {}", fileList.size(), remoteUploadDirectoryPath);

        return remoteUploadDirectoryPath;
    }

    /**
     * Derives the remote (server) paths for all files and populates an
     * ordered list of directories that need to be created.
     * This method is protected to support testing.
     *
     * @param  localRootPath              common parent path for local files
     *                                    (should end with the file separator).
     * @param  localFilePaths             list of local file paths
     *                                    (should only contain files - no directories).
     * @param  remoteUploadDirectoryPath  base upload directory path on remote server.
     * @param  orderedDirectoryPaths      ordered list of directories to be created on server
     *                                    (returned to caller).
     * @param  remotePathToFileMap        map of derived remote paths to local files
     *                                    (returned to caller).
     *
     * @throws IllegalArgumentException
     *   if any path cannot be derived.
     */
    protected void derivePaths(String localRootPath,
                               List<String> localFilePaths,
                               String remoteUploadDirectoryPath,
                               List<String> orderedDirectoryPaths,
                               Map<String, File> remotePathToFileMap)
            throws IllegalArgumentException {

        Set<String> relativeDirectoryPaths = new HashSet<String>(localFilePaths.size());

        final int relativeStart = localRootPath.length();

        String relativePath;
        String remotePath;
        for (String localPath : localFilePaths) {
            if ((localPath.length() > relativeStart) && (localPath.startsWith(localRootPath))) {
                relativePath = localPath.substring(relativeStart);
                relativePath = relativePath.replace('\\', '/');
                addDirectoryPaths(relativePath, relativeDirectoryPaths);
                remotePath = remoteUploadDirectoryPath + relativePath;
                remotePathToFileMap.put(remotePath, new File(localPath));
            } else {
                throw new IllegalArgumentException(
                        localPath + " does not start with specified root " + localRootPath);
            }
        }

        for (String relativeDirPath : relativeDirectoryPaths) {
            orderedDirectoryPaths.add(remoteUploadDirectoryPath + relativeDirPath);
        }

        // sort the paths so that parent directories are created before children
        Collections.sort(orderedDirectoryPaths);
    }

    private void addDirectoryPaths(String relativePath,
                                   Set<String> relativeDirectoryPaths) {
        int endIndex = relativePath.indexOf('/');
        String dirPath;
        while (endIndex != -1) {
            endIndex++;
            dirPath = relativePath.substring(0, endIndex);
            relativeDirectoryPaths.add(dirPath);
            endIndex = relativePath.indexOf('/', endIndex);
        }
    }

    private void uploadFiles(Map<String, File> remotePathToFileMap)
            throws WebDavException {
        for (String path : remotePathToFileMap.keySet()) {
            uploadFile(path, remotePathToFileMap.get(path));
        }
    }

    private void createDirectory(String path)
            throws WebDavException {
        final URL url = getWebDavUrl(path);
        client.createDirectory(url);
    }

    private void uploadFile(String path,
                            File file)
            throws WebDavException {
        final URL url = getWebDavUrl(path);
        client.saveFile(url, file);
    }

    private URL getWebDavUrl(String path)
            throws WebDavException {
        URL url;
        try {
            // replace spaces with '-' so that launch of external tools is not broken
            url = client.getWebDavUrl(path.replace(' ','-'));
        } catch (MalformedURLException e) {
            throw new WebDavException("failed to create URL for " + path, e);
        }
        return url;
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebDavUploader.class);

    private static final String JACS_ROOT_UPLOAD_PATH = "/groups/scicomp/jacsData/upload";

}
