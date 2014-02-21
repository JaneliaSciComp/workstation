package org.janelia.it.FlyWorkstation.shared.util.filecache;

/**
 * Thrown when WebDAV information cannot be retrieved or saved.
 *
 * @author Eric Trautman
 */
public class WebDavException extends RuntimeException {

    private Integer statusCode;

    public WebDavException(String message) {
        super(message);
    }

    public WebDavException(String message,
                           int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public WebDavException(String message,
                           Throwable cause) {
        super(message, cause);
    }

    public WebDavException(String message,
                           Throwable cause,
                           Integer statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

}
