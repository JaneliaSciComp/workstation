package org.janelia.it.FlyWorkstation.shared.util.filecache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

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
     * @param  rootRemoteUploadPath
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
     * @return the server path for the uploaded file.
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

        return remoteFilePath;
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
            url = client.getWebDavUrl(path);
        } catch (MalformedURLException e) {
            throw new WebDavException("failed to create URL for " + path, e);
        }
        return url;
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebDavUploader.class);

    private static final String JACS_ROOT_UPLOAD_PATH = "/groups/scicomp/jacsData/upload";

}
