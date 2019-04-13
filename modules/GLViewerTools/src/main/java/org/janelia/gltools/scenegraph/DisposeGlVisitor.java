package org.janelia.gltools.scenegraph;

import javax.media.opengl.GL3;

/**
 *
 * @author Christopher Bruns
 */
class DisposeGlVisitor implements NodeVisitor
{
    private GL3 gl;

    public DisposeGlVisitor(GL3 gl)
    {
        this.gl = gl;
    }

    @Override
    public void visit(DrawableNode node)
    {
        if (node == null)
            return;
        node.dispose(gl);
    }

    @Override
    public void visit(SceneNode node)
    {}

}
