package org.janelia.it.workstation.gui.opengl;

import javax.media.opengl.GL2;

import org.janelia.it.jacs.shared.geom.Rotation3d;
import org.janelia.it.jacs.shared.geom.Quaternion.AngleAxis;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.viewer3d.ViewportGL;

// GLOrthoCamera sets up orthographic view for OpenGL from a Camera3d
public class GLOrthoCamera 
{
	protected Camera3d camera;
	protected ViewportGL viewport;
	protected boolean isPushed = false; // Help manage RAII in crippled Java language
    // Viewer orientation relative to canonical orientation.
    // Canonical orientation is x-right, y-down, z-away
	protected Rotation3d viewerInGround = new Rotation3d();
	
	public Rotation3d getViewerInGround() {
		return viewerInGround;
	}

	public void setViewerInGround(Rotation3d viewerInGround) {
		this.viewerInGround = viewerInGround;
	}

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
		// FW-2823: this assert was triggering a lot in dev; it's disabled in
		// 	production with no apparent ill effect, so disable here; leave
		//	this comment in case we want to figure out what is going wrong later
		// assert ! isPushed;
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
		// Apply viewer rotation
		// TODO - before or after translation?
		AngleAxis angleAxis = viewerInGround.inverse().convertRotationToAngleAxis();
		gl.glRotated(angleAxis.angle * 180.0/Math.PI, 
				angleAxis.axis.x(),
				angleAxis.axis.y(),
				angleAxis.axis.z());
		// 
		gl.glTranslated(-f.x(),-f.y(),-f.z());
		isPushed = true;
	}
	
	public void tearDown(GL2 gl) {
		if (! isReady())
			return;
		// disabled; see comment at top of setUp()
		// assert isPushed;
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		isPushed = false;
	}
}
