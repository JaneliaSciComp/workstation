package org.janelia.it.FlyWorkstation.shared.util.filecache;

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

    private URL url;
    private boolean isDirectory;
    private Long contentLength;
    private String etag;

    /**
     * Parses the specified WebDAV PROPFIND response 'fragment' to
     * populate this file's attributes.
     *
     * @param  baseUrl              the full URL for the originally requested
     *                              resource (could be a parent directory).
     * @param  multiStatusResponse  the PROPFIND response for the file.
     *
     * @throws IllegalArgumentException
     *   if a file specific URL cannot be constructed.
     */
    public WebDavFile(URL baseUrl,
                      MultiStatusResponse multiStatusResponse)
            throws IllegalArgumentException {

        final int baseLength = baseUrl.getPath().length();
        final String href = multiStatusResponse.getHref();
        final int hrefLength = href.length();

        if (hrefLength == baseLength) {
            this.url = baseUrl;
        } else if (hrefLength > baseLength) {
            try {
                this.url = new URL(baseUrl, href.substring(baseLength));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(
                        "failed to construct file URL from baseUrl " +
                        baseUrl + " and href " + href, e);
            }
        } else {
            throw new IllegalArgumentException("invalid href value '" + href +
                                               "' returned for resource with base URL " + baseUrl);
        }

        final DavPropertySet propertySet =
                multiStatusResponse.getProperties(HttpStatus.SC_OK);

        final DefaultDavProperty resourceTypeProperty =
                (DefaultDavProperty) propertySet.get(DavPropertyName.RESOURCETYPE);

        if (resourceTypeProperty != null) {
            final String resourceTypeValue =
                    String.valueOf(resourceTypeProperty.getValue());
            this.isDirectory = (resourceTypeValue.contains(COLLECTION));
        }

        final DavProperty contentLengthProperty =
                propertySet.get(DavPropertyName.GETCONTENTLENGTH);
        if (contentLengthProperty != null) {
            this.contentLength = Long.parseLong(
                    String.valueOf(contentLengthProperty.getValue()));
        }

        final DavProperty etagProperty =
                propertySet.get(DavPropertyName.GETETAG);
        if (etagProperty != null) {
            this.etag = String.valueOf(etagProperty.getValue());
        }

    }

    /**
     * Constructs a WebDavFile instance that references the specified file.
     *
     * @param  url   the remote URL for this file.
     * @param  file  a copy of the remote file referenced by this instance.
     */
    public WebDavFile(URL url,
                      File file) {
        if (url == null) {
            try {
                this.url = file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(
                        "failed to create URL for " + file.getAbsolutePath());
            }
        } else {
            this.url = url;
        }
        this.isDirectory = file.isDirectory();
        this.contentLength = file.length();
        this.etag = "not-available";
    }

    /**
     * @return the file's URL.
     */
    public URL getUrl() {
        return url;
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
        return "\nWebDavFile{url='" + url + '\'' +
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
