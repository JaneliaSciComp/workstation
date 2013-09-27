package org.janelia.it.FlyWorkstation.gui.opengl;

import java.awt.Color;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

public class SolidBackgroundActor 
implements GLActor
{
    private Color color;
    
    public SolidBackgroundActor(Color color) {
        this.color = color;
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
        GL gl = glDrawable.getGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        return null;
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
        GL gl = glDrawable.getGL();
        gl.glClearColor(
                color.getRed()/255.0f,
                color.getGreen()/255.0f,
                color.getBlue()/255.0f,
                color.getAlpha()/255.0f);
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) 
    {}

}
