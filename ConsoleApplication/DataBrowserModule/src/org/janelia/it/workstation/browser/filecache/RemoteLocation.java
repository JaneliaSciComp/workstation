package org.janelia.it.workstation.browser.filecache;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Encapsulates minimum amount of information for a remote file.
 */
public class RemoteLocation {

    private String remoteStorageURL;
    private final String remoteFilePath;
    private final String remoteFileUrl;

    RemoteLocation(String remoteFilePath, String remoteFileUrl) {
        this.remoteFilePath = remoteFilePath;
        this.remoteFileUrl = remoteFileUrl;
    }

    public String getRemoteStorageURL() {
        return remoteStorageURL;
    }

    public void setRemoteStorageURL(String remoteStorageURL) {
        this.remoteStorageURL = remoteStorageURL;
    }

    public String getRemoteFilePath() {
        return remoteFilePath;
    }

    public String getRemoteFileUrl() {
        return remoteFileUrl;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("remoteFilePath", remoteFilePath)
                .append("remoteFileUrl", remoteFileUrl)
                .toString();
    }
}
