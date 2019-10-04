package org.janelia.workstation.core.filecache;

import java.nio.file.Path;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.janelia.filecacheutils.FileKey;
import org.janelia.filecacheutils.LocalFileCacheStorage;

public class WebdavCachedFileKey implements FileKey {

    private final String remoteFileName;

    public WebdavCachedFileKey(String remoteFileName) {
        this.remoteFileName = remoteFileName;
    }

    @Override
    public Path getLocalPath(LocalFileCacheStorage localFileCacheStorage) {
        return localFileCacheStorage.getLocalFileCacheDir().resolve(getCachedFileName());
    }

    String getRemoteFileName() {
        return remoteFileName;
    }

    String getRemoteFileScheme() {
        if (remoteFileName.startsWith("jade://")) {
            return "jade";
        } else if (remoteFileName.startsWith("file://")) {
            return "file";
        } else if (remoteFileName.startsWith("http://") || remoteFileName.startsWith("https://")) {
            return "http";
        } else {
            return "";
        }
    }

    private String getCachedFileName() {
        return RegExUtils.removeFirst(StringUtils.replaceChars(remoteFileName, '\\', '/'), "^((.+:)?/+)+"); // replace patterns like C://, file:///D:/, //
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        WebdavCachedFileKey that = (WebdavCachedFileKey) o;

        return new EqualsBuilder()
                .append(remoteFileName, that.remoteFileName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(remoteFileName)
                .toHashCode();
    }
}
