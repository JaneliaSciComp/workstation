package org.janelia.it.FlyWorkstation.gui.opengl;

import java.awt.Color;

import javax.media.opengl.GL;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

/**
 * Should work for both GL2 and GL3
 * @author brunsc
 *
 */
public class SolidBackgroundActor 
implements GL3Actor
{
    private Color color;
    
    public SolidBackgroundActor(Color color) {
        this.color = color;
    }

    @Override
    public void display(GLActorContext context) {
        GL gl = context.getGLAutoDrawable().getGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        return null;
    }

    @Override
    public void init(GLActorContext context) {
        GL gl = context.getGLAutoDrawable().getGL();
        gl.glClearColor(
                color.getRed()/255.0f,
                color.getGreen()/255.0f,
                color.getBlue()/255.0f,
                color.getAlpha()/255.0f);
    }

    @Override
    public void dispose(GLActorContext context) 
    {}

}
