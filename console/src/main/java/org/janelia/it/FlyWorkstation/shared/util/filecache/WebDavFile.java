package org.janelia.it.FlyWorkstation.shared.util.filecache;

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.property.*;

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

    /**
     * Parses the specified WebDAV PROPFIND response 'fragment' to
     * populate this file's attributes.
     *
     * @param  directoryUrl         the full URL for the directory that
     *                              contains the file.
     * @param  multiStatusResponse  the PROPFIND response for the file.
     *
     * @throws IllegalArgumentException
     *   if a file specific URL cannot be constructed.
     */
    public WebDavFile(URL directoryUrl,
                      MultiStatusResponse multiStatusResponse)
            throws IllegalArgumentException {

        int baseLength = directoryUrl.getPath().length();
        final String href = multiStatusResponse.getHref();

        if (href.length() > baseLength) {
            try {
                this.url = new URL(directoryUrl, href.substring(baseLength));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(
                        "failed to construct file URL from directoryUrl " +
                        directoryUrl + " and href " + href, e);
            }
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

    }

    /**
     * Constructs a WebDavFile from a local file (for testing).
     *
     * @param  file  local file to wrap.
     */
    protected WebDavFile(File file) {
        try {
            this.url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                    "failed to create URL for " + file.getAbsolutePath());
        }
        this.isDirectory = file.isDirectory();
        this.contentLength = file.length();
    }

    public URL getUrl() {
        return url;
    }

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

    @Override
    public String toString() {
        return "\nWebDavFile{url='" + url + '\'' +
                ", isDirectory=" + isDirectory +
                ", contentLength=" + contentLength +
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
        PROPERTY_NAMES = nameSet;
    }
}
