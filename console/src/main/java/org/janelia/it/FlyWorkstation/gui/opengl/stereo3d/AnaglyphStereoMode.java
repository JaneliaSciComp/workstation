package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import org.janelia.it.FlyWorkstation.gui.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;

public class AnaglyphStereoMode extends AbstractStereoMode 
{
	private boolean leftRed, leftGreen, leftBlue;
	
    public AnaglyphStereoMode(
    		ObservableCamera3d camera, 
    		GLActor monoActor,
    		boolean leftRed, boolean leftGreen, boolean leftBlue)
	{
    	super(camera, monoActor);
    	this.leftRed = leftRed;
    	this.leftGreen = leftGreen;
    	this.leftBlue = leftBlue;
	}

	public void display(GLAutoDrawable glDrawable) {
	    final GL2 gl = glDrawable.getGL().getGL2();
	    // Left eye red
		gl.glColorMask(leftRed, leftGreen, leftBlue, true);
		super.clear(glDrawable);
		setLeftEyeView(gl);
		paintScene(glDrawable);
		// Right eye cyan
		gl.glColorMask(!leftRed, !leftGreen, !leftBlue, true);
		super.clear(glDrawable);
		setRightEyeView(gl);
		paintScene(glDrawable);
		// Restore full color
		gl.glColorMask(true, true, true, true);
	}
}
