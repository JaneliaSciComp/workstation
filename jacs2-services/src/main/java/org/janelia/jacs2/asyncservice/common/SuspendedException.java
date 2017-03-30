package org.janelia.jacs2.asyncservice.common;

/**
 * Exception thrown when a service was suspended.
 */
public class SuspendedException extends RuntimeException {

    public SuspendedException() {
    }

    public SuspendedException(Throwable cause) {
        super(cause);
    }

}
