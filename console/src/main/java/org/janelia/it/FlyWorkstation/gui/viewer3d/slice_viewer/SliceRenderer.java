package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BaseRenderer;
import org.janelia.it.FlyWorkstation.gui.viewer3d.ViewportGL;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.GLOrthoCamera;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;

public class SliceRenderer
extends BaseRenderer
{
	ViewportGL viewport = new ViewportGL();
	GLOrthoCamera glCamera = new GLOrthoCamera(camera);
	
	public SliceRenderer() {
		glCamera.setCamera(camera);
		glCamera.setViewport(viewport);
	}

	public ViewportGL getViewport() {
		return viewport;
	}

	public void setViewport(ViewportGL viewport) {
		this.viewport = viewport;
	}

    @Override
    public void display(GLAutoDrawable gLDrawable) 
    {
        final GL2 gl = gLDrawable.getGL().getGL2();
        displayBackground(gl);
        // set camera
        glCamera.setUp(gl);
        for (GLActor a : actors) {
        		a.display(gl);
        }
        glCamera.tearDown(gl);
    		gl.glFlush();
    }

    @Override
    public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) 
    {
        final GL2 gl = gLDrawable.getGL().getGL2();
        viewport.reshape(gl, width, height);
    }
    
    @Override
    public void setCamera(Camera3d camera) {
    		super.setCamera(camera);
    		glCamera.setCamera(camera);
    }
}
