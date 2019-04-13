package org.janelia.workstation.integration.spi.domain;

/**
 * Implement this for a compatibility check.
 * 
 * @author fosterl
 */
public interface Compatible<T> {
    
    boolean isCompatible(T object);
    
}
