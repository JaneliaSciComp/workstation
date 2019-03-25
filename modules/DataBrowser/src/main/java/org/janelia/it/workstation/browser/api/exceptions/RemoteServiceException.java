package org.janelia.it.workstation.browser.api.exceptions;

public class RemoteServiceException extends RuntimeException {

    public RemoteServiceException(String message) {
        super(message);
    }
    
    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}