package org.janelia.it.FlyWorkstation.gui.viewer3d.camera;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.ViewportGL;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;

// GLOrthoCamera sets up orthographic view for OpenGL from a Camera3d
public class GLOrthoCamera 
{
	protected Camera3d camera;
	protected ViewportGL viewport;
	protected boolean isPushed = false; // Help manage RAII in crippled Java language
	
	public ViewportGL getViewport() {
		return viewport;
	}

	public void setViewport(ViewportGL viewport) {
		this.viewport = viewport;
	}

	public GLOrthoCamera(Camera3d camera) {
		this.camera = camera;
	}

	@Override
	protected void finalize() {
		if (isPushed)
			System.err.println("Error: GLOrthoCamera was not torn down properly");
	}
	
	public Camera3d getCamera() {
		return camera;
	}

	protected boolean isReady() {
		if (camera == null)
			return false;
		if (viewport == null)
			return false;
		int w = viewport.getWidth();
		int h = viewport.getHeight();
		if (w*h == 0)
			return false;
		return true;
	}

	public void setCamera(Camera3d camera) {
		this.camera = camera;
	}
	
	public void setUp(GL2 gl) {
		if (! isReady())
			return;
		assert ! isPushed;
		// set viewport size - TODO - maybe only do this in viewport reshape method
		int w = viewport.getWidth();
		int h = viewport.getHeight();
		gl.glViewport(0, 0, w, h);
		// projection matrix
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		// Put origin at center of viewport
		int hw = w/2;
		int hh = h/2;
		if (hw == 0)
			hw = 1;
		if (hh == 0)
			hh = 1;
		int d = viewport.getDepth();
		int hd = d/2;
		if (hd == 0)
			hd = 1;
		// glOrtho() is where OpenGL Y convention (Bottom origin) gets flipped to
		// image Y convention (Top origin).
		// (Flip Z too, to keep right handed)
		gl.glOrtho(-hw, hw, hh, -hh, hd, -hd);
		// model/view matrix
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		// zoom
		double s = camera.getPixelsPerSceneUnit();
		gl.glScaled(s, s, s);
		// translate
		Vec3 f = camera.getFocus();
		// System.out.println("glTranslated "+f.z());
		gl.glTranslated(-f.x(),-f.y(),-f.z());
		isPushed = true;
	}
	
	public void tearDown(GL2 gl) {
		if (! isReady())
			return;
		assert isPushed;
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		isPushed = false;
	}
}
