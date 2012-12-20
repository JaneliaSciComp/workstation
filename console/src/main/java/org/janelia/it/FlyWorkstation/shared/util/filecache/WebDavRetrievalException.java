package org.janelia.it.FlyWorkstation.shared.util.filecache;

/**
 * Thrown when WebDAV information cannot be retrieved.
 *
 * @author Eric Trautman
 */
public class WebDavRetrievalException extends RuntimeException {

    private Integer statusCode;

    public WebDavRetrievalException(String message) {
        super(message);
    }

    public WebDavRetrievalException(String message,
                                    int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public WebDavRetrievalException(String message,
                                    Throwable cause) {
        super(message, cause);
    }

    public Integer getStatusCode() {
        return statusCode;
    }

}
