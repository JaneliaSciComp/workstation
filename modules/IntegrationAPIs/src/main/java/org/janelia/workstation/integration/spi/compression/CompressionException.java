package org.janelia.workstation.integration.spi.compression;

/**
 * Throw this in event compression fails in either a predictable way,
 * or as the result of some other operation having thrown an exception.
 *
 * @author fosterl
 */
public class CompressionException extends Exception {
    /**
     * Failing due to some condition having not been met.
     * @param message tell why
     */
    public CompressionException(String message) {
        super(message);
    }
    
    /**
     * Failing due to caught exception.
     * @param parent caught
     */
    public CompressionException(Exception parent) {
        super(parent);
    }    
}
