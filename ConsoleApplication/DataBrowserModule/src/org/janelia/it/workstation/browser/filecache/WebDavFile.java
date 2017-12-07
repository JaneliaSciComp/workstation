package org.janelia.it.workstation.browser.filecache;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Encapsulates minimum amount of information for a remote
 * file accesible through WebDAV.
 *
 * @author Eric Trautman
 */
public class WebDavFile {

    private String remoteFileUrl;
    private String webdavFileKey;
    private boolean isDirectory;
    private Long contentLength;
    private String etag;

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
    WebDavFile(String webdavFileKey, MultiStatusResponse multiStatusResponse)
            throws IllegalArgumentException {

        this.webdavFileKey = webdavFileKey;
        remoteFileUrl = multiStatusResponse.getHref();
        final DavPropertySet propertySet =
                multiStatusResponse.getProperties(HttpStatus.SC_OK);

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

        final DavProperty<?> etagProperty =
                propertySet.get(DavPropertyName.GETETAG);
        if (etagProperty != null) {
            this.etag = String.valueOf(etagProperty.getValue());
        }
    }

    /**
     * @return remote href as extracted from the multiresponse
     */
    public URL getRemoteFileUrl() {
        try {
            return new URL(remoteFileUrl);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return the webdav file key
     */
    public String getWebdavFileKey() {
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
    public long getKilobytes() {
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

    /**
     * @return the file's etag.
     */
    public String getEtag() {
        return etag;
    }

    @Override
    public String toString() {
        return "\nWebDavFile{webdavFileKey='" + webdavFileKey + '\'' +
                ", isDirectory=" + isDirectory +
                ", contentLength=" + contentLength +
                ", etag=" + etag +
                '}';
    }

    private static final long ONE_KILOBYTE = 1024;
    private static final String COLLECTION = "collection";

    /**
     * The set of WebDAV properties required to populate a
     * {@link WebDavFile} instance.
     */
    public static final DavPropertyNameSet PROPERTY_NAMES;
    static {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DavPropertyName.RESOURCETYPE);
        nameSet.add(DavPropertyName.GETCONTENTLENGTH);
        nameSet.add(DavPropertyName.GETETAG);
        PROPERTY_NAMES = nameSet;
    }
}
