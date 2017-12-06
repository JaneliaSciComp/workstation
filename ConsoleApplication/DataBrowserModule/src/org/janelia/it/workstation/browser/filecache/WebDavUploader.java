package org.janelia.it.workstation.browser.filecache;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to upload files to the server via WebDAV.
 *
 * @author Eric Trautman
 */
public class WebDavUploader {
    private static final Logger LOG = LoggerFactory.getLogger(WebDavUploader.class);

    private final WebDavClientMgr webDavClientMgr;
    private String uploadClientHostAddress;
    private String uploadClientStartTimestamp;
    private volatile long uploadCount;

    /**
     * Constructs an uploader.
     *
     * @param webDavClientMgr WebDAV client manager for the current session
     */
    public WebDavUploader(WebDavClientMgr webDavClientMgr) {
        this.webDavClientMgr = webDavClientMgr;
        InetAddress address;
        try {
            address = InetAddress.getLocalHost();
            this.uploadClientHostAddress = address.getHostAddress();
        } catch (UnknownHostException e) {
            this.uploadClientHostAddress = "unknown";
            LOG.warn("failed to derive client host address, ignoring error", e);
        }
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");
        this.uploadClientStartTimestamp = sdf.format(new Date());

        this.uploadCount = 0;
    }

    /**
     * Uploads the specified file to the server.
     *
     * @param  file  file to upload.
     *
     * @return the server path for the parent directory of the uploaded file.
     *
     * @throws WebDavException
     *   if the file cannot be uploaded.
     */
    public String uploadFile(File file) throws WebDavException {
        final String storageName = createStorageName(file);
        String storageURL = webDavClientMgr.createStorage(storageName);
        webDavClientMgr.uploadFile(file.toPath());
        LOG.info("uploaded {} to {}", file, storageURL);
        return storageURL;
    }

    private String createStorageName(File f) {
        StringBuilder path = new StringBuilder(128);
        path.append(uploadClientStartTimestamp);
        path.append("__");
        path.append(uploadClientHostAddress);
        path.append("__");
        path.append(uploadCount++);
        path.append("__");
        path.append(f.getName());
        return path.toString();
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
    public String uploadFiles(List<File> fileList, File localRootDirectory)
            throws IllegalArgumentException, WebDavException {

        final String storageName = createStorageName(localRootDirectory);
        String storageURL = webDavClientMgr.createStorageDirectory(storageName);

        // need to go through the entire fileList and create the directory hierarchy
        // and then upload the file content
        Path localRootPath = localRootDirectory.toPath();
        Set<Path> filePathHierarchy = fileList.stream()
                .filter(f -> f.isFile())
                .map(f -> f.getParentFile().toPath().relativize(localRootPath))
                .flatMap(fp -> {
                    int nPathComponents = fp.getNameCount();
                    return IntStream.range(1, nPathComponents)
                            .mapToObj(pathIndex -> fp.subpath(0, pathIndex));
                })
                .sorted()
                .collect(Collectors.toSet());

        filePathHierarchy.forEach(webDavClientMgr::createDirectory);
        fileList.stream()
                .filter(f -> f.isFile())
                .map(f -> f.toPath().relativize(localRootPath))
                .forEach(webDavClientMgr::uploadFile);

        LOG.info("uploaded {} files to {}", fileList.size(), storageURL);
        return storageURL;
    }

}
