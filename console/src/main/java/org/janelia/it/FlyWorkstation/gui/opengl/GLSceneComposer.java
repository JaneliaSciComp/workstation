package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import org.janelia.it.FlyWorkstation.geom.Rotation3d;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.stereo3d.*;
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
    private StereoMode anaglyphGreenMagentaStereoMode = new AnaglyphGreenMagentaStereoMode();
    private StereoMode anaglyphRedCyanStereoMode = new AnaglyphRedCyanStereoMode();
    private StereoMode hardwareStereoMode = new HardwareStereoMode();
    private StereoMode leftEyeStereoMode = new LeftEyeStereoMode();
    private StereoMode rightEyeStereoMode = new RightEyeStereoMode();
    private StereoMode leftRightStereoMode = new LeftRightStereoMode();    
    private StereoMode monoStereoMode = new MonoStereoMode();
    private StereoMode stereoMode = monoStereoMode;

	private static final Vec3 upInCamera = new Vec3(0,-1,0);
	private ObservableCamera3d camera;

	private CameraScreenGeometry cameraScreenGeometry;

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
	    // double screenEyeDistanceCm = 70.0; // varies per seat
	    // Library Dell U2713H 2560x1440 pixels; 59.67x33.57 cm; => 42.9 pixels/cm
	    // double screenPixelsPerCm = 42.9; // varies per monitor
	    cameraScreenGeometry = new CameraScreenGeometry(camera,
	            70.0, // screenEyeDistanceCm
	            42.9, // screenPixelsPerCm
	            6.2); // intraOcularDistanceCm
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
            updateModelViewMatrix(actorContext);
	        viewChanged = false;
	    }
	    stereoMode.display(actorContext, this);
	}

	public void displayBackground(GLActorContext actorContext) {
	    backgroundActors.display(actorContext);
	}
	
    public void displayOpaque(GLActorContext actorContext) {
        opaqueActors.display(actorContext);
    }
    
    public void displayTransparent(GLActorContext actorContext) {
        transparentActors.display(actorContext);
    }
    
    public void displayHud(GLActorContext actorContext) {
        hudActors.display(actorContext);
    }
    
    public void displayScene(GLActorContext actorContext) {
        if (useDepth) {
            GL gl = actorContext.getGLAutoDrawable().getGL();
            gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        }
        // Render in 4 passes
        displayBackground(actorContext);
        displayOpaque(actorContext);
        displayTransparent(actorContext);
        displayHud(actorContext);
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
	    GL2GL3 gl2gl3 = gl.getGL2GL3();
	    // Use sRGB framebuffer for correct lighting on computer screens
	    gl2gl3.glEnable(GL2GL3.GL_FRAMEBUFFER_SRGB);
	    // 
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
	    stereoMode.reshape(glDrawable, x, y, width, height);
		viewChanged = true;
	}

	protected void updateModelViewMatrix(GLActorContext actorContext) {
		GL2Adapter ga = actorContext.getGL2Adapter();
		ga.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
		ga.glLoadIdentity();
		Vec3 f = camera.getFocus();
	    Rotation3d g_R_c = camera.getRotation();
	    Vec3 u_g = g_R_c.times(upInCamera);
		// Distance from user/camera to screen/focal point
		double camDistCm = cameraScreenGeometry.getScreenEyeDistanceCm(); // screenEyeDistanceCm;
		double camDistPx = camDistCm * cameraScreenGeometry.getScreenPixelsPerCm(); // screenPixelsPerCm;
	    double camDistScene = camDistPx / camera.getPixelsPerSceneUnit();
	    Vec3 c = f.plus(g_R_c.times(new Vec3(0,0,-camDistScene)));
	    ga.gluLookAt(c.x(), c.y(), c.z(), // camera in ground
	            f.x(), f.y(), f.z(), // focus in ground
	            u_g.x(), u_g.y(), u_g.z()); // up vector in ground
	}

    public CameraScreenGeometry getCameraScreenGeometry() {
        return cameraScreenGeometry;
    }

}
