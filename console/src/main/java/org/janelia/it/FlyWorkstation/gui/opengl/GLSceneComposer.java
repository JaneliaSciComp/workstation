package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL;
// import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import org.janelia.it.FlyWorkstation.geom.Rotation3d;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.GL2Adapter.MatrixMode;
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
    
    private GL3Actor[] allActors = {
            backgroundActors,
            opaqueActors,
            transparentActors,
            hudActors,
    };

    private GLAutoDrawable glComponent;
    GL2Adapter gl2Adapter = null;
    
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

    public void addBackgroundActor(GL3Actor actor) {
        backgroundActors.addActor(actor);        
    }

    public void addOpaqueActor(GL3Actor actor) {
        opaqueActors.addActor(actor);
    }

	@Override
	public void display(GLAutoDrawable glDrawable) {
	    GLActorContext actorContext = new GLActorContext(glDrawable, gl2Adapter);
	    if (viewChanged) {
            // GL2 gl2 = glDrawable.getGL().getGL2();
	        updateViewport(glDrawable);
            updateProjectionMatrix(actorContext);
            updateModelViewMatrix(actorContext);
	        viewChanged = false;
	    }
	    // Render in 4 passes
	    if (useDepth) {
	        GL gl = glDrawable.getGL();
	        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
	    }
	    for (GL3Actor actor : allActors)
	        actor.display(actorContext);
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
        GLActorContext actorContext = new GLActorContext(glDrawable, gl2Adapter);
	    for (GL3Actor actor : allActors)
	        actor.dispose(actorContext);
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
	    final GL gl = glDrawable.getGL();
	    gl2Adapter = GL2AdapterFactory.createGL2Adapter(glDrawable);
	    GLActorContext actorContext = new GLActorContext(glDrawable, gl2Adapter);
		if (useDepth)
			gl.glEnable(GL.GL_DEPTH_TEST);
		for (GL3Actor actor : allActors)
		    actor.init(actorContext);
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
		updateProjectionMatrix(new GLActorContext(glDrawable, gl2Adapter));
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
	protected void updateProjectionMatrix(GLActorContext actorContext) {
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

	    GL2Adapter ga = actorContext.getGL2Adapter();
	    ga.glMatrixMode(MatrixMode.GL_PROJECTION);
		ga.glLoadIdentity();

		// TODO - probably don't need both atan2() and tan()...
		double top = zNear * Math.tan(fovy/360*Math.PI);
		double right = aspect * top;
		ga.glFrustum(
				-right, right,
		        -top, top,
		        zNear, zFar);
		
		ga.glMatrixMode(MatrixMode.GL_MODELVIEW);
	}

	protected void updateModelViewMatrix(GLActorContext actorContext) {
		GL2Adapter ga = actorContext.getGL2Adapter();
		ga.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
		ga.glLoadIdentity();
		Vec3 f = camera.getFocus();
	    Rotation3d g_R_c = camera.getRotation();
	    Vec3 u_g = g_R_c.times(upInCamera);
		// Distance from user/camera to screen/focal point
		double camDistCm = screenEyeDistanceCm;
		double camDistPx = camDistCm * screenPixelsPerCm;
	    double camDistScene = camDistPx / camera.getPixelsPerSceneUnit();
	    Vec3 c = f.plus(g_R_c.times(new Vec3(0,0,-camDistScene)));
	    ga.gluLookAt(c.x(), c.y(), c.z(), // camera in ground
	            f.x(), f.y(), f.z(), // focus in ground
	            u_g.x(), u_g.y(), u_g.z()); // up vector in ground
	}

}
