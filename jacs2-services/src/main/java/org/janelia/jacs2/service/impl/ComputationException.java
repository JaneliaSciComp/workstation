package org.janelia.jacs2.service.impl;

/**
 * Exception thrown by a service if something goes wrong during computation.
 */
public class ComputationException extends RuntimeException {
    private JacsService<?> jacsService;

    public ComputationException(JacsService jacsService) {
        this.jacsService = jacsService;
    }

    public ComputationException(JacsService jacsService, String message) {
        super(message);
        this.jacsService = jacsService;
    }

    public ComputationException(JacsService jacsService, Throwable cause) {
        super(cause);
        this.jacsService = jacsService;
    }

}
