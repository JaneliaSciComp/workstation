package org.janelia.gltools.scenegraph;

import java.util.Collection;
import org.janelia.geometry3d.Sphere;

/**
 *
 * @author Christopher Bruns
 */
public interface SceneNode
{
    SceneNode getParent();
    boolean addChild(SceneNode child);
    Collection<? extends SceneNode> getChildren();
    
    Sphere getBoundingSphere();
    void setBoundingSphereDirty();
    boolean boundingSphereIsDirty();
    
    void accept(NodeVisitor visitor);
}
