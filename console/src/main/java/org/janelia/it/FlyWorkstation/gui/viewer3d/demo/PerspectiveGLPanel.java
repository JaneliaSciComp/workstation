package org.janelia.it.FlyWorkstation.gui.viewer3d.demo;

import java.awt.Dimension;
import java.util.List;
import java.util.Vector;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.GLU;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot;

@SuppressWarnings("serial")
class PerspectiveGLPanel extends GLJPanel
implements GLEventListener
{
    private GLU glu = new GLU();
    private List<GLActor> actors = new Vector<GLActor>();
    private ObservableCamera3d camera = null;
    private boolean cameraChanged = false;
    
    private Vec3 upInCamera = new Vec3(0,-1,0);
    private double cameraDistanceInPixels = 2000;

    private Slot onCameraChangedSlot = new Slot() {
		@Override
		public void execute() {
			cameraChanged = true;
			repaint();
		}
    };
    
	PerspectiveGLPanel() {
		setPreferredSize(new Dimension(400,400));
		ObservableCamera3d c = new BasicObservableCamera3d();
		c.setFocus(new Vec3(0, 0, 0));
		c.setPixelsPerSceneUnit(100);
		setCamera(c);
		addGLEventListener(this);
	}

	public void addActor(GLActor actor) {
		actors.add(actor);
	}
	
	@Override
	public void display(GLAutoDrawable glDrawable) {
        final GL2 gl = glDrawable.getGL().getGL2();
        if (cameraChanged) {
        	updateProjectionMatrix(gl, getWidth(), getHeight());
        	updateModelViewMatrix(gl);
        	cameraChanged = false;
        }
        for (GLActor actor : actors)
        	actor.display(gl);
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
        final GL2 gl = glDrawable.getGL().getGL2();
        for (GLActor actor : actors)
        	actor.dispose(gl);
	}

	public ObservableCamera3d getCamera() {
		return camera;
	}

	public void setCamera(ObservableCamera3d camera) {
		if (this.camera == camera)
			return;
		if (this.camera != null)
			this.camera.getFocusChangedSignal().disconnect(onCameraChangedSlot);
		this.camera = camera;
		this.camera.getViewChangedSignal().connect(onCameraChangedSlot);
		cameraChanged = true;
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
		cameraChanged = true;
        final GL2 gl = glDrawable.getGL().getGL2();
        for (GLActor actor : actors)
        	actor.init(gl);
	}

	@Override
	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) 
	{
        final GL2 gl = glDrawable.getGL().getGL2();
		updateProjectionMatrix(gl, width, height);
	}
	
	private void updateProjectionMatrix(GL2 gl, int w, int h) {
		w = Math.max(w, 1);
		h = Math.max(h, 1);
		gl.glViewport(0, 0, w, h);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		double aspect = w/(double)h;
        double camDist = cameraDistanceInPixels / camera.getPixelsPerSceneUnit();
        double tanx = cameraDistanceInPixels;
        double tany = h / 2.0;
        double fovy = 2 * Math.atan2(tany, tanx) * 180 / Math.PI;
	    glu.gluPerspective(fovy, // fovy
	    		aspect,
	    		0.3 * camDist, // znear
	    		3.0 * camDist); // zfar
		gl.glMatrixMode(GL2.GL_MODELVIEW);
	}
	
	private void updateModelViewMatrix(GL2 gl) {
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		Vec3 f = camera.getFocus();
        Rotation3d g_R_c = camera.getRotation();
        Vec3 u_g = g_R_c.times(upInCamera);
        double camDist = cameraDistanceInPixels / camera.getPixelsPerSceneUnit();
        Vec3 c = f.plus(g_R_c.times(new Vec3(0,0,-camDist)));
        glu.gluLookAt(c.x(), c.y(), c.z(), // camera in ground
                f.x(), f.y(), f.z(), // focus in ground
                u_g.x(), u_g.y(), u_g.z()); // up vector in ground
	}
}