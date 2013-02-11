package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BaseRenderer;
import org.janelia.it.FlyWorkstation.gui.viewer3d.GLActor;

public class SliceRenderer
extends BaseRenderer
{
	public SliceRenderer() {
		actors.add(new TileActor());
	}

    @Override
    public void display(GLAutoDrawable gLDrawable) 
    {
    		super.display(gLDrawable);
        final GL2 gl = gLDrawable.getGL().getGL2();
        for (GLActor a : actors) {
        		a.display(gl);
        }
    		gl.glFlush();
    }

    @Override
    public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) 
    {
        final GL2 gl = gLDrawable.getGL().getGL2();
    		gl.glViewport(0, 0, width, height);
    }
}
