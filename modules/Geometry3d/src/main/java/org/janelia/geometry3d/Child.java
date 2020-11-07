package org.janelia.geometry3d;

/**
 *
 * @author Christopher Bruns
 */
public interface Child<E>
{
    E getParent();
    E setParent(E parent);
}
