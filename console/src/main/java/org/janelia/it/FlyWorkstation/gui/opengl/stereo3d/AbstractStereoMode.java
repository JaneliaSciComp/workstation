package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import java.awt.Color;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.signal.Signal;
import org.janelia.it.FlyWorkstation.signal.Slot;

public class AbstractStereoMode 
implements GLEventListener
{
	
	// Adjustable parameters for viewing geometry
	// CMB 6.2 cm;
	protected double intraOcularDistanceCm = 6.2; // varies per user
	protected double screenEyeDistanceCm = 70.0; // varies per seat
	// Library Dell U2713H 2560x1440 pixels; 59.67x33.57 cm; => 42.9 pixels/cm
	protected double screenPixelsPerCm = 42.9; // varies per monitor
	
	private GLU glu = new GLU();
	private Vec3 upInCamera = new Vec3(0,-1,0);
	// private double cameraDistanceInPixels = 2000;
	private ObservableCamera3d camera;
	private boolean viewChanged = false;
	private boolean swapEyes = false;

	private Color backgroundColor = null;
	private boolean useDepth = true;
	private GLEventListener monoActor;
	
	protected int viewportWidth = 1;
	protected int viewportHeight = 1;

	private Slot onCameraChangedSlot = new Slot() {
			@Override
			public void execute() {
				setViewChanged(true);
				viewChangedSignal.emit();
			}
	    };
	    
	public Signal viewChangedSignal = new Signal();

	public AbstractStereoMode(ObservableCamera3d camera, 
			GLEventListener monoActor)
	{
		super();
		setCamera(camera);
		setMonoActor(monoActor);
		setBackgroundColor(Color.gray);
	}

	protected void clear(GLAutoDrawable glDrawable) {
	    final GL2 gl = glDrawable.getGL().getGL2();
	    int clearMask = 0;
	    if (backgroundColor != null) {
	    	clearMask |= GL2.GL_COLOR_BUFFER_BIT;
	    	gl.glClearColor(
	    			backgroundColor.getRed()/255.0f,
	    			backgroundColor.getGreen()/255.0f,
	    			backgroundColor.getBlue()/255.0f,
	    			backgroundColor.getAlpha()/255.0f);
	    }
	    if (useDepth)
	    	clearMask |= GL2.GL_DEPTH_BUFFER_BIT;
	    if (clearMask != 0)
	    	gl.glClear(clearMask);
	    if (isViewChanged()) {
	    	updateViewport(gl);
	    	updateProjectionMatrix(gl, 0.0);
	    	updateModelViewMatrix(gl);
	    	setViewChanged(false);
	    }
	}
	
	@Override
	public void display(GLAutoDrawable glDrawable) {
		clear(glDrawable);
	    paintScene(glDrawable);
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
		if (monoActor != null)
			monoActor.dispose(glDrawable);
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Color backgroundColor) {
		if (this.backgroundColor == backgroundColor)
			return;
		this.backgroundColor = backgroundColor;
	}

	public Camera3d getCamera() {
		return camera;
	}

	public GLEventListener getMonoActor() {
		return monoActor;
	}

	public void setMonoActor(GLEventListener monoActor) {
		this.monoActor = monoActor;
	}

	public boolean isViewChanged() {
		return viewChanged;
	}

	public void setViewChanged(boolean viewChanged) {
		this.viewChanged = viewChanged;
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
	    final GL2 gl = glDrawable.getGL().getGL2();
		if (useDepth)
			gl.glEnable(GL2.GL_DEPTH_TEST);
		if (monoActor != null)
			monoActor.init(glDrawable);
	}

	public boolean isSwapEyes() {
		return swapEyes;
	}

	public void setSwapEyes(boolean swapEyes) {
		if (this.swapEyes == swapEyes) {
			return; // no change
		}
		this.swapEyes = swapEyes;
		setViewChanged(true);
	}

	protected void paintScene(GLAutoDrawable glDrawable) {
		if (monoActor != null)
			monoActor.display(glDrawable);		
	}

	@Override
	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width,
			int height) 
	{
		int w = Math.max(width, 1);
		int h = Math.max(height, 1);
		if ((w == viewportWidth) && (h == viewportHeight))
			return; // no change
		viewportWidth = w;
		viewportHeight = h;
	    final GL2 gl = glDrawable.getGL().getGL2();
	    updateViewport(gl);
		updateProjectionMatrix(gl, 0.0);
		viewChanged = true;
	}

	public void setCamera(ObservableCamera3d camera) {
		if (this.camera == camera)
			return;
		if (this.camera != null)
			this.camera.getFocusChangedSignal().disconnect(onCameraChangedSlot);
		this.camera = camera;
		this.camera.getViewChangedSignal().connect(onCameraChangedSlot);
		viewChanged = true;
	}

	protected void setCenterEyeView(GL2 gl) {
		updateProjectionMatrix(gl, 0);
	}
	
	protected void setLeftEyeView(GL2 gl) {
		updateProjectionMatrix(gl, -1);
	}
	
	protected void setRightEyeView(GL2 gl) {
		updateProjectionMatrix(gl, 1);
	}
	
	protected void updateViewport(GL2 gl) {
		gl.glViewport(0, 0, viewportWidth, viewportHeight);
	}

	/**
	 * 
	 * @param gl
	 * @param eye -1 for left eye, +1 for right eye, 0 for mono
	 */
	protected void updateProjectionMatrix(GL2 gl, double eye) {
		if (isSwapEyes())
			eye = -eye;
		
		int w = viewportWidth;
		int h = viewportHeight;
		double aspect = w/(double)h;
		// Convert between 3 coordinate systems:
		//  1) real world/user/lab in units of centimeters (cm)
		//  2) monitor screen in units of pixels (px)
		//  3) scene world in scene units (scene)
		// Distance from user/camera to screen/focal point
		double camDistCm = screenEyeDistanceCm;
		double camDistPx = camDistCm * screenPixelsPerCm;
	    double camDistScene = camDistPx / camera.getPixelsPerSceneUnit();
	    double tanx = camDistPx;
	    double tany = h / 2.0;
	    double fovy = 2 * Math.atan2(tany, tanx) * 180 / Math.PI; // degrees
	    double zNear = 0.3 * camDistScene;
	    double zFar = 3.0 * camDistScene;
	    // Distance between the viewers eyes
	    double iodPx = intraOcularDistanceCm * screenPixelsPerCm;
	    double iodScene = iodPx / camera.getPixelsPerSceneUnit();

	    gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		// TODO - probably don't need both atan2() and tan()...
		double top = zNear * Math.tan(fovy/360*Math.PI);
		double right = aspect * top;

		double frustumShift = -eye * (iodScene/2) * (zNear/camDistScene);
		
		double modelTranslation = -eye * iodScene/2; // for right eye
		gl.glFrustum(
				-right + frustumShift, right + frustumShift,
		        -top, top,
		        zNear, zFar);
		
		// TODO - does moving translation to modelview improve
		// lighting? Especially with raycast shader...
		gl.glTranslated(modelTranslation, 0, 0);

		gl.glMatrixMode(GL2.GL_MODELVIEW);
	}

	protected void updateModelViewMatrix(GL2 gl) {
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		Vec3 f = camera.getFocus();
	    Rotation3d g_R_c = camera.getRotation();
	    Vec3 u_g = g_R_c.times(upInCamera);
		// Distance from user/camera to screen/focal point
		double camDistCm = screenEyeDistanceCm;
		double camDistPx = camDistCm * screenPixelsPerCm;
	    double camDistScene = camDistPx / camera.getPixelsPerSceneUnit();
	    Vec3 c = f.plus(g_R_c.times(new Vec3(0,0,-camDistScene)));
	    glu.gluLookAt(c.x(), c.y(), c.z(), // camera in ground
	            f.x(), f.y(), f.z(), // focus in ground
	            u_g.x(), u_g.y(), u_g.z()); // up vector in ground
	}

}