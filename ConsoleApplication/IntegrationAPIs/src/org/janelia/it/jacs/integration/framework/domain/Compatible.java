package org.janelia.it.jacs.integration.framework.domain;

/**
 * Implement this for a compatibility check.
 * 
 * @author fosterl
 */
public interface Compatible<T> {
    
    boolean isCompatible(T object);
    
}