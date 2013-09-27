package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.FlyWorkstation.gui.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;

public class CrossEyeStereoMode extends AbstractStereoMode 
{
    public CrossEyeStereoMode(
    		ObservableCamera3d camera, 
    		GLActor monoActor)
	{
    	super(camera, monoActor);
	}

	public void display(GLAutoDrawable glDrawable) {
		// Clear color and depth just once for both views
		super.clear(glDrawable);
	    // Right eye view on left
	    final GL2 gl = glDrawable.getGL().getGL2();
		gl.glViewport(0, 0, viewportWidth, viewportHeight);
		setRightEyeView(gl);
		paintScene(glDrawable);
		// Left eye view on right
		gl.glViewport(viewportWidth, 0, viewportWidth, viewportHeight);
		setLeftEyeView(gl);
		paintScene(glDrawable);
	}

	@Override
	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width,
			int height) 
	{
		// Use only half width at a time.
		int w = Math.max(width/2, 1);
		int h = Math.max(height, 1);
		if ((w == viewportWidth) && (h == viewportHeight))
			return; // no change
		viewportWidth = w;
		viewportHeight = h;
	    final GL2 gl = glDrawable.getGL().getGL2();
	    updateViewport(gl);
		updateProjectionMatrix(gl, 0.0);
		setViewChanged(true);
	}
}
