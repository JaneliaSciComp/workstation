package org.janelia.gltools.scenegraph;

import javax.media.opengl.GL3;

/**
 *
 * @author Christopher Bruns
 */
public interface DrawableNode
{
    void draw(RenderInfo renderInfo);

    void dispose(GL3 gl);
}
