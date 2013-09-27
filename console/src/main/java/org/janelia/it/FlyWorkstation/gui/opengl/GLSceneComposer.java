package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

import org.janelia.it.FlyWorkstation.geom.Rotation3d;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.signal.Signal;
import org.janelia.it.FlyWorkstation.signal.Slot;

/**
 * GLSceneComposer has two responsibilities:
 *  1) Manage order of actor presentation for correct use of depth buffer.
 *  2) Manage projection/model/view matrices from camera settings.
 *  
 * These capabilities are just fine grained enough to be leveraged for 
 * stereoscopic viewing in derived classes.
 * 
 * @author brunsc
 *
 */
public class GLSceneComposer 
implements GLEventListener
{
	private static final Vec3 upInCamera = new Vec3(0,-1,0);
    private static final GLU glu = new GLU();
    private GL2Adapter gl2Adapter;

	private ObservableCamera3d camera;
	
	// Screen geometry TODO - does this belong in its own class?
	protected int viewportWidth = 1;
	protected  int viewportHeight = 1;
    protected double screenEyeDistanceCm = 70.0; // varies per seat
    // Library Dell U2713H 2560x1440 pixels; 59.67x33.57 cm; => 42.9 pixels/cm
    protected double screenPixelsPerCm = 42.9; // varies per monitor

	private boolean useDepth = true;
	
	private CompositeGLActor backgroundActors = new CompositeGLActor();
    private CompositeGLActor opaqueActors = new CompositeGLActor();
    private CompositeGLActor transparentActors = new CompositeGLActor();
    private CompositeGLActor hudActors = new CompositeGLActor();
    
    private GLActor[] allActors = {
            backgroundActors,
            opaqueActors,
            transparentActors,
            hudActors,
    };

    private GLAutoDrawable glComponent;
    
    private boolean viewChanged = true;
    private Slot onViewChangedSlot = new Slot() {
        @Override
        public void execute() {
            viewChanged = true;
            viewChangedSignal.emit();
        }
    };
    
    public Signal viewChangedSignal = new Signal();

	public GLSceneComposer(ObservableCamera3d camera, GLAutoDrawable component)
	{
	    this.camera = camera;
	    camera.getViewChangedSignal().connect(onViewChangedSlot);
	    //
	    this.glComponent = component;
	    component.addGLEventListener(this);
        viewChangedSignal.connect(new Slot() {
            @Override
            public void execute() {
                    glComponent.display();
            }});
	}

    public void addBackgroundActor(GLActor actor) {
        backgroundActors.addActor(actor);        
    }

    public void addOpaqueActor(GLActor actor) {
        opaqueActors.addActor(actor);
    }

	@Override
	public void display(GLAutoDrawable glDrawable) {
	    if (viewChanged) {
            // GL2 gl2 = glDrawable.getGL().getGL2();
	        updateViewport(glDrawable);
            updateProjectionMatrix(glDrawable);
            updateModelViewMatrix(glDrawable);
	        viewChanged = false;
	    }
	    // Render in 4 passes
	    if (useDepth) {
	        GL gl = glDrawable.getGL();
	        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
	    }
	    for (GLActor actor : allActors)
	        actor.display(glDrawable);
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
	    for (GLActor actor : allActors)
	        actor.dispose(glDrawable);
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
	    final GL gl = glDrawable.getGL();
	    gl2Adapter = GL2AdapterFactory.createGL2Adapter(glDrawable);
		if (useDepth)
			gl.glEnable(GL.GL_DEPTH_TEST);
		for (GLActor actor : allActors)
		    actor.init(glDrawable);
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
	    updateViewport(glDrawable);
		updateProjectionMatrix(glDrawable);
		viewChanged = true;
	}

	protected void updateViewport(GLAutoDrawable glDrawable) {
	    GL gl = glDrawable.getGL();
		gl.glViewport(0, 0, viewportWidth, viewportHeight);
	}

	/**
	 * 
	 * @param gl
	 * @param eye -1 for left eye, +1 for right eye, 0 for mono
	 */
	protected void updateProjectionMatrix(GLAutoDrawable glDrawable) {
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
	    // TODO - expose near/far clip planes
	    double zNear = 0.3 * camDistScene;
	    double zFar = 3.0 * camDistScene;

	    // TODO - get the GL2 out of here, for GL3
	    GL2 gl = glDrawable.getGL().getGL2();
	    gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		// TODO - probably don't need both atan2() and tan()...
		double top = zNear * Math.tan(fovy/360*Math.PI);
		double right = aspect * top;
		gl.glFrustum(
				-right, right,
		        -top, top,
		        zNear, zFar);
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
	}

	protected void updateModelViewMatrix(GLAutoDrawable glDrawable) {
        // TODO - get the GL2 out of here, for GL3
        GL2 gl = glDrawable.getGL().getGL2();
        /*
		gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
		gl2Adapter.glLoadIdentity();
		*/
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        //
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