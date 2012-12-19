package org.janelia.it.FlyWorkstation.shared.util.filecache;

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.property.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;

/**
 * TODO: add javadoc
 *
 * @author Eric Trautman
 */
public class WebDavFile {

    private URL url;
    private boolean isDirectory;
    private Long contentLength;

    public WebDavFile(URL baseUrl,
                      MultiStatusResponse multiStatusResponse)
            throws IllegalArgumentException {

        int baseLength = baseUrl.getPath().length();
        final String href = multiStatusResponse.getHref();

        if (href.length() > baseLength) {
            try {
                this.url = new URL(baseUrl, href.substring(baseLength));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(
                        "failed to construct file URL from baseUrl " +
                        baseUrl + " and href " + href, e);
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

    public static final Comparator<WebDavFile> LENGTH_COMPARATOR = new Comparator<WebDavFile>() {
        @Override
        public int compare(WebDavFile o1, WebDavFile o2) {
            if (o1.contentLength == null) {
                if (o2.contentLength == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (o2.contentLength == null) {
                return 1;
            } else {
                return o1.contentLength.compareTo(o2.contentLength);
            }
        }
    };

    public static final DavPropertyNameSet PROPERTY_NAMES;
    static {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DavPropertyName.RESOURCETYPE);
        nameSet.add(DavPropertyName.GETCONTENTLENGTH);
        PROPERTY_NAMES = nameSet;
    }
}
