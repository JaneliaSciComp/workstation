package org.janelia.workstation.core.api.exceptions;

public class RemoteServiceException extends RuntimeException {

    public RemoteServiceException(String message) {
        super(message);
    }
    
    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}