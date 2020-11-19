package org.janelia.workstation.integration.spi.domain;

/**
 * Implement this for a compatibility check.
 * 
 * @author fosterl
 */
public interface Compatible<T> {

    boolean isCompatible(Class<? extends T> object);

    boolean isCompatible(T object);
    
}
