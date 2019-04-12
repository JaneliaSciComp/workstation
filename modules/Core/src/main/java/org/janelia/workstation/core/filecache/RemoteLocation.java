package org.janelia.workstation.core.filecache;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Encapsulates minimum amount of information for a remote file.
 */
public class RemoteLocation {

    private String storageURL;
    private final String virtualFilePath;
    private final String realFilePath;
    private final String fileUrl;

    RemoteLocation(String virtualFilePath, String realFilePath, String fileUrl) {
        this.virtualFilePath = virtualFilePath;
        this.realFilePath = realFilePath;
        this.fileUrl = fileUrl;
    }

    public String getStorageURL() {
        return storageURL;
    }

    public void setStorageURL(String storageURL) {
        this.storageURL = storageURL;
    }

    public String getVirtualFilePath() {
        return virtualFilePath;
    }

    public String getRealFilePath() {
        return realFilePath;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("storageURL", storageURL)
                .append("virtualFilePath", virtualFilePath)
                .append("realFilePath", realFilePath)
                .append("fileUrl", fileUrl)
                .toString();
    }
}
