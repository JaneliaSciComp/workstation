package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import org.janelia.it.FlyWorkstation.gui.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;

public class HardwareStereoMode extends AbstractStereoMode 
{
	private boolean stereoIsEnabled = false;
	
    public HardwareStereoMode(
    		ObservableCamera3d camera, 
    		GLActor monoActor)
	{
    	super(camera, monoActor);
	}

    @Override
	public void display(GLAutoDrawable glDrawable) {
	    final GL2 gl = glDrawable.getGL().getGL2();
		if (stereoIsEnabled) {
		    // Left eye
		    gl.glDrawBuffer(GL2.GL_BACK_LEFT);
			super.clear(glDrawable);
			setLeftEyeView(gl);
			paintScene(glDrawable);
			// Right eye
		    gl.glDrawBuffer(GL2.GL_BACK_RIGHT);
			super.clear(glDrawable);
			setRightEyeView(gl);
			paintScene(glDrawable);
			// Restore default double buffer mode
		    gl.glDrawBuffer(GL2.GL_BACK);
		}
		else {
			super.display(glDrawable);
		}
	}
    
    @Override
    public void init(GLAutoDrawable glDrawable) {
    	super.init(glDrawable);
		GLCapabilitiesImmutable glCaps = glDrawable.getChosenGLCapabilities();
		stereoIsEnabled = glCaps.getStereo();
		if (! stereoIsEnabled) {
			System.out.println("Stereo 3D not available");
		}
    }
}
