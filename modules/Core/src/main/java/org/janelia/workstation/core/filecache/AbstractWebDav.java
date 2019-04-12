package org.janelia.workstation.core.filecache;

import org.apache.commons.lang.StringUtils;
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
     * @param  multiStatusResponse  the PROPFIND response for the file.
     *
     * @throws IllegalArgumentException
     *   if a file specific URL cannot be constructed.
     */
    AbstractWebDav(String webdavFileKey, MultiStatusResponse multiStatusResponse)
            throws IllegalArgumentException {

        this.webdavFileKey = webdavFileKey;

        final DavPropertySet notFoundPropertySet = multiStatusResponse.getProperties(HttpStatus.SC_NOT_FOUND);

        if (!notFoundPropertySet.isEmpty()) {
            // the property was not found - this can be either because the file or storage does not exist
            // or because there are no storage agents to serve the requested content
            throw new IllegalArgumentException("No resource found for " + webdavFileKey +
                    ". This happens either because no storage was found or " +
                    "because there are no storage agents to serve the requested content.");
        } else {
            final DavPropertySet propertySet = multiStatusResponse.getProperties(HttpStatus.SC_OK);

            remoteFileUrl = multiStatusResponse.getHref();
            final DefaultDavProperty<?> resourceTypeProperty =
                    (DefaultDavProperty<?>) propertySet.get(DavPropertyName.RESOURCETYPE);

            if (resourceTypeProperty != null) {
                final String resourceTypeValue =
                        String.valueOf(resourceTypeProperty.getValue());
                this.isDirectory = (resourceTypeValue.contains(COLLECTION));
            }

            final DavProperty<?> contentLengthProperty =
                    propertySet.get(DavPropertyName.GETCONTENTLENGTH);
            if (contentLengthProperty != null) {
                String contentLengthStrValue = (String) contentLengthProperty.getValue();
                if (StringUtils.isNotBlank(contentLengthStrValue)) {
                    this.contentLength = Long.parseLong(contentLengthStrValue);
                }
            }

            final DavProperty<?> storageRootDirProperty =
                    propertySet.get(DavPropertyName.create("storageRootDir", Namespace.getNamespace("JADE:")));
            if (storageRootDirProperty != null) {
                this.storageRootDir = String.valueOf(storageRootDirProperty.getValue());
            }

            final DavProperty<?> storageBindNameProperty =
                    propertySet.get(DavPropertyName.create("storageBindName", Namespace.getNamespace("JADE:")));
            if (storageBindNameProperty != null) {
                this.storageBindName = String.valueOf(storageBindNameProperty.getValue());
            }

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
    long getKilobytes() {
        long kilobytes = 0;
        if (contentLength != null) {
            final long len = contentLength;
            kilobytes = len / ONE_KILOBYTE;
            if ((len % ONE_KILOBYTE) > 0) {
                kilobytes++; // round up to nearest kb
            }
        }
        return kilobytes;
    }

    String getStorageRootDir() {
        return storageRootDir;
    }

    String getStorageBindName() {
        return storageBindName;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{webdavFileKey='" + webdavFileKey + '\'' +
                ", isDirectory=" + isDirectory +
                ", contentLength=" + contentLength +
                '}';
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
