package org.janelia.console.viewerapi;

/**
 *
 * @author Christopher Bruns
 */
public interface Copyable<T> {
    void copy(T rhs);
}
