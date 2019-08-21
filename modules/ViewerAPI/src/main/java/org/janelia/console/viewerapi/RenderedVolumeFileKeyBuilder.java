package org.janelia.console.viewerapi;

import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.filecacheutils.FileProxy;

class RenderedVolumeFileKeyBuilder {

    private final String renderedVolumePath;
    private String absolutePath;
    private String relativePath;
    private List<String> channelImageNames;
    private int pageNumber = -1;

    RenderedVolumeFileKeyBuilder(String renderedVolumePath) {
        this.renderedVolumePath = renderedVolumePath;
    }

    RenderedVolumeFileKey build(Supplier<FileProxy> fileProxySupplier) {
        StringBuilder builder = new StringBuilder();
        builder.append(normalizePath(renderedVolumePath));
        appendPath(builder, absolutePath);
        appendPath(builder, relativePath);
        String channels;
        if (channelImageNames != null) {
            channels = channelImageNames.stream()
                    .map(cin -> normalizePath(cin))
                    .filter(cin -> StringUtils.isNotBlank(cin))
                    .reduce((c1, c2) -> c1 + "+" + c2)
                    .orElse("");
        } else {
            channels = "";
        }
        if (StringUtils.isNotBlank(channels)) {
            builder.append('/').append(channels);
        }
        if (pageNumber >= 0) {
            builder.append('.').append(pageNumber);
        }
        return new RenderedVolumeFileKey(builder.toString(), fileProxySupplier);
    }


    RenderedVolumeFileKeyBuilder withRelativePath(String relativePath) {
        this.relativePath = relativePath;
        return this;
    }

    RenderedVolumeFileKeyBuilder withChannelImageNames(List<String> channelImageNames) {
        this.channelImageNames = channelImageNames;
        return this;
    }

    RenderedVolumeFileKeyBuilder withPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    RenderedVolumeFileKeyBuilder withAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
        return this;
    }

    private String normalizePath(String p) {
        // remove the start and the end '/' if it exists
        return StringUtils.removeStart(
                StringUtils.removeEnd(StringUtils.replaceChars(p, '\\', '/'), "/"),
                "/"
        );
    }

    private StringBuilder appendPath(StringBuilder b, String p) {
        String normalizedPath = normalizePath(p);
        if (StringUtils.isNotBlank(normalizedPath)) {
            b.append('/').append(normalizedPath);
        }
        return b;
    }
}
