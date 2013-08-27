package org.janelia.it.FlyWorkstation.gui.viewer3d.demo;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;

import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;

public class HardwareStereoMode extends AbstractStereoMode 
{
	private boolean stereoIsEnabled = false;
	
    public HardwareStereoMode(
    		ObservableCamera3d camera, 
    		GLEventListener monoActor)
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
