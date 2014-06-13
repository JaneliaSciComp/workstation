package org.janelia.it.workstation.nb_action;

/**
 * Implement this for a compatibility check.
 * 
 * @author fosterl
 */
public interface Compatible<T> {
    boolean isCompatible( T object );
}
