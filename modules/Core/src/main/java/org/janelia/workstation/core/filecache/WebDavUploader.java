package org.janelia.workstation.core.filecache;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to upload files to the server via WebDAV.
 *
 * @author Eric Trautman
 */
public class WebDavUploader {
    private static final Logger LOG = LoggerFactory.getLogger(WebDavUploader.class);

    private final StorageClientMgr storageClientMgr;

    /**
     * Constructs an uploader.
     *
     * @param storageClientMgr WebDAV client manager for the current session
     */
    public WebDavUploader(StorageClientMgr storageClientMgr) {
        this.storageClientMgr = storageClientMgr;
    }

    public String createUploadContext(String contextName, String subjectName, String storageTags) {
        if (shouldIncludeUserFolder(storageTags) || StringUtils.isBlank(subjectName)) {
            return contextName;
        } else {
            return subjectName + "/" + contextName;
        }
    }

    /**
     * A storage is considered to already be user aware if it has the tag "includesUserFolder"
     * @param storageTags
     * @return
     */
    private boolean shouldIncludeUserFolder(String storageTags) {
        if (storageTags == null) {
            return false;
        } else {
            for (String tag : Splitter.on(',').trimResults().omitEmptyStrings().split(storageTags)) {
                if ("includesUserFolder".equalsIgnoreCase(tag)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Uploads the specified file to the server.
     *
     * @param  storageName user assigned storage name.
     * @param  storageContext storage path context.
     * @param  storageTags tags used for selecting the storage.
     * @param  file  file to upload.
     *
     * @return the server path for the parent directory of the uploaded file.
     *
     * @throws WebDavException
     *   if the file cannot be uploaded.
     */
    public RemoteLocation uploadFile(String storageName, String storageContext, String storageTags, File file) throws WebDavException {
        String storageURL = storageClientMgr.createStorage(storageName, storageContext, storageTags);
        RemoteLocation remoteFile = storageClientMgr.uploadFile(file, storageURL, storageClientMgr.urlEncodeComp(file.getName()));
        LOG.info("Uploaded file {}", file);
        LOG.info("  realFilePath: {}", remoteFile.getRealFilePath());
        LOG.info("  storageUrl: {}", remoteFile.getStorageURL());
        LOG.info("  virtualFilePath: {}", remoteFile.getVirtualFilePath());
        LOG.info("  fileUrl: {}", remoteFile.getFileUrl());
        return remoteFile;
    }

    /**
     * Uploads the specified files to the server.
     *
     * @param  storageName         user assigned storage name
     * @param  fileList            list of local files to upload.
     * @param  storageContext storage path context.
     * @param  storageTags tags used for selecting the storage.
     * @param  localRootDirectory  a common parent of all listed files that is used to determine
     *                             the relative path for each file on the server.
     *                             For example given files /a/b/f1.txt and /a/b/c/f2.txt
     *                             and a localRootDirectory of /a, the server paths would be:
     *                             [upload-dir]/b/f1.txt and [upload-dir]/b/c/f2.txt.
     *                             This parameter is optional.
     *                             Specifiy null if the relative paths are not important AND
     *                             all file names are unique.
     *
     * @return the list of uploaded files.
     *
     * @throws IllegalArgumentException
     *   if the server paths cannot be derived or are not unique.
     *
     * @throws WebDavException
     *   if the files cannot be uploaded.
     */
    public List<RemoteLocation> uploadFiles(String storageName, String storageContext, String storageTags, List<File> fileList, File localRootDirectory)
            throws IllegalArgumentException, WebDavException {

        String storageURL = storageClientMgr.createStorage(storageName, storageContext, storageTags);
        Path localRootPath = localRootDirectory.toPath();
        List<RemoteLocation> remoteFileList = fileList.stream()
                .filter(f -> f.isFile())
                .map(f -> {
                    RemoteLocation remoteFile = storageClientMgr.uploadFile(f, storageURL, storageClientMgr.urlEncodeComps(localRootPath.relativize(f.toPath()).toString()));
                    LOG.info("uploaded {} to {} - {}", f, storageURL, remoteFile);
                    return remoteFile;
                })
                .collect(Collectors.toList());

        LOG.info("uploaded {} files to {}", fileList.size(), storageURL);
        return remoteFileList;
    }

}
