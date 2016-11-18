package org.janelia.jacs2.service.impl;

/**
 * Exception thrown by a service if something goes wrong during computation.
 */
public class ComputationException extends RuntimeException {
    public ComputationException() {
    }

    public ComputationException(String message) {
        super(message);
    }

    public ComputationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComputationException(Throwable cause) {
        super(cause);
    }

    public ComputationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
