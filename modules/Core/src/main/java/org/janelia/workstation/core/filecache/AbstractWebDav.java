package org.janelia.workstation.core.filecache;

import com.sun.org.apache.xpath.internal.operations.Mult;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.xml.Namespace;

/**
 * Encapsulates minimum amount of information for a remote
 * file accesible through WebDAV.
 *
 * @author Eric Trautman
 */
class AbstractWebDav {

    private String remoteFileUrl;
    private String webdavFileKey;
    private boolean isDirectory;
    private Long contentLength;
    private String storageRootDir;
    private String storageBindName;

    /**
     * Parses the specified WebDAV PROPFIND response 'fragment' to
     * populate this file's attributes.
     *
     * @param  webdavFileKey        the webdav file key
     * @param  multiStatusResponses  the PROPFIND responses for the file.
     *
     * @throws IllegalArgumentException
     *   if a file specific URL cannot be constructed.
     */
    AbstractWebDav(String webdavFileKey, MultiStatusResponse[] multiStatusResponses)
            throws IllegalArgumentException {

        this.webdavFileKey = webdavFileKey;

        boolean resourceFound = false;
        for (MultiStatusResponse multiStatusResponse : multiStatusResponses) {
            final DavPropertySet goodResource = multiStatusResponse.getProperties(HttpStatus.SC_OK);
            if (goodResource.isEmpty()) {
                continue;
            } else {
                resourceFound = true;
                remoteFileUrl = multiStatusResponse.getHref();
                final DefaultDavProperty<?> resourceTypeProperty =
                        (DefaultDavProperty<?>) goodResource.get(DavPropertyName.RESOURCETYPE);

                if (resourceTypeProperty != null) {
                    final String resourceTypeValue =
                            String.valueOf(resourceTypeProperty.getValue());
                    this.isDirectory = (resourceTypeValue.contains(COLLECTION));
                }

                final DavProperty<?> contentLengthProperty =
                        goodResource.get(DavPropertyName.GETCONTENTLENGTH);
                if (contentLengthProperty != null) {
                    String contentLengthStrValue = (String) contentLengthProperty.getValue();
                    if (StringUtils.isNotBlank(contentLengthStrValue)) {
                        this.contentLength = Long.parseLong(contentLengthStrValue);
                    }
                }

                final DavProperty<?> storageRootDirProperty =
                        goodResource.get(DavPropertyName.create("storageRootDir", Namespace.getNamespace("JADE:")));
                if (storageRootDirProperty != null) {
                    this.storageRootDir = String.valueOf(storageRootDirProperty.getValue());
                }

                final DavProperty<?> storageBindNameProperty =
                        goodResource.get(DavPropertyName.create("storageBindName", Namespace.getNamespace("JADE:")));
                if (storageBindNameProperty != null) {
                    this.storageBindName = String.valueOf(storageBindNameProperty.getValue());
                }
                break;
            }
        }

        if (!resourceFound) {
            // the property was not found - this can be either because the file or storage does not exist
            // or because there are no storage agents to serve the requested content
            throw new IllegalArgumentException("No resource found for " + webdavFileKey +
                    ". This happens either because no storage was found or " +
                    "because there are no storage agents to serve the requested content.");
        }
    }

    /**
     * @return remote href as extracted from the multiresponse
     */
    String getRemoteFileUrl() {
        return remoteFileUrl;
    }

    /**
     * @return the webdav file key
     */
    String getWebdavFileKey() {
        return webdavFileKey;
    }

    /**
     * @return true if the file si a directory; otherwise false.
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    /**
     * @return the number of kilobytes in file.
     */
    Long getSizeInBytes() {
        if (contentLength != null) {
            return contentLength;
        } else {
            return null;
        }
    }

    String getStorageRootDir() {
        return storageRootDir;
    }

    String getStorageBindName() {
        return storageBindName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("remoteFileUrl", remoteFileUrl)
                .append("webdavFileKey", webdavFileKey)
                .append("isDirectory", isDirectory)
                .append("contentLength", contentLength)
                .append("storageRootDir", storageRootDir)
                .append("storageBindName", storageBindName)
                .toString();
    }

    private static final long ONE_KILOBYTE = 1024;
    private static final String COLLECTION = "collection";

    /**
     * The set of WebDAV properties required to populate a
     * {@link AbstractWebDav} instance.
     */
    static final DavPropertyNameSet PROPERTY_NAMES;
    static {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DavPropertyName.RESOURCETYPE);
        nameSet.add(DavPropertyName.GETCONTENTLENGTH);
        PROPERTY_NAMES = nameSet;
    }
}
