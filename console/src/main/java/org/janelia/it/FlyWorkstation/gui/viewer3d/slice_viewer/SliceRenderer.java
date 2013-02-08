package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BaseRenderer;

public class SliceRenderer 
extends BaseRenderer
{
    @Override
    public void display(GLAutoDrawable gLDrawable) 
    {
    		super.display(gLDrawable);
        final GL2 gl = gLDrawable.getGL().getGL2();
    		gl.glFlush();
    }

    @Override
    public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) 
    {
        final GL2 gl = gLDrawable.getGL().getGL2();
    		gl.glViewport(0, 0, width, height);
    }
}
