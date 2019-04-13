package org.janelia.gltools.scenegraph;

/**
 *
 * @author Christopher Bruns
 */
public interface NodeVisitor
{
    void visit(DrawableNode node);
    void visit(SceneNode node);
}
