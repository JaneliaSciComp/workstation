package org.janelia.console.viewerapi;

/**
 *
 * @author Christopher Bruns
 */
public interface Child<E>
{
    E getParent();
    E setParent(E parent);
}
