package org.janelia.gltools.scenegraph;

import java.util.ArrayList;
import java.util.Collection;
import org.janelia.geometry3d.Sphere;

/**
 *
 * @author Christopher Bruns
 */
public class BasicSceneNode implements SceneNode
{
    private SceneNode parent;
    private final Collection<SceneNode> children = new ArrayList<>();
    private boolean boundingSphereIsDirty = true;

    public BasicSceneNode(SceneNode parent) {
        this.parent = parent;
    }
    
    @Override
    public SceneNode getParent()
    {
        return parent;
    }

    @Override
    public Collection<? extends SceneNode> getChildren()
    {
        return children;
    }

    @Override
    public Sphere getBoundingSphere()
    {
        return null;
    }

    @Override
    public void setBoundingSphereDirty()
    {
        boundingSphereIsDirty = true;
    }

    @Override
    public boolean boundingSphereIsDirty()
    {
        return boundingSphereIsDirty;
    }

    @Override
    public void accept(NodeVisitor visitor)
    {
        visitor.visit(this);
        for (SceneNode node : children) {
            node.accept(visitor);
        }
    }

    @Override
    public boolean addChild(SceneNode child)
    {
        return children.add(child);
    }
    
}
