package org.janelia.it.FlyWorkstation.shared.util.filecache;

/**
 * Thrown when WebDAV information cannot be retrieved.
 *
 * @author Eric Trautman
 */
public class WebDavRetrievalException extends RuntimeException {

    public WebDavRetrievalException(String message,
                                    Throwable cause) {
        super(message, cause);
    }
}
