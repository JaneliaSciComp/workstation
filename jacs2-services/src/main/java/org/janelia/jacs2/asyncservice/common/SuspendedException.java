package org.janelia.jacs2.asyncservice.common;

/**
 * Exception thrown when a service was suspended.
 */
public class SuspendedException extends RuntimeException {

    public SuspendedException() {
    }

    public SuspendedException(String message) {
        super(message);
    }

    public SuspendedException(Throwable cause) {
        super(cause);
    }

}
