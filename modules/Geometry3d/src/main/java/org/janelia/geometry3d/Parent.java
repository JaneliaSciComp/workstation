package org.janelia.geometry3d;

import java.util.Collection;

/**
 *
 * @author Christopher Bruns
 */
public interface Parent<E>
{
    E addChild(E child);
    Collection<? extends E> getChildren();
}
