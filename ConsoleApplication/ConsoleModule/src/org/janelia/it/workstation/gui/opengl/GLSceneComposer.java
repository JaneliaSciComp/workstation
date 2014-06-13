package org.janelia.it.workstation.gui.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.ObservableCamera3d;
import org.janelia.it.workstation.gui.opengl.stereo3d.MonoStereoMode;
import org.janelia.it.workstation.gui.opengl.stereo3d.StereoMode;
import org.janelia.it.workstation.signal.Signal;
import org.janelia.it.workstation.signal.Slot;
import org.janelia.it.workstation.signal.Slot1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    private static Logger logger = LoggerFactory.getLogger( GLSceneComposer.class );
    protected static GLU glu = new GLU();

    private StereoMode stereoMode = new MonoStereoMode();
    boolean stereoModeNeedsCleanup = true; // for Mac

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

	public Slot1<StereoMode> setStereoModeSlot = new Slot1<StereoMode>() {
		@Override
		public void execute(StereoMode mode) {
			setStereoMode(mode);
		}
	};

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

    public void addTransparentActor(GL3Actor actor) {
        transparentActors.addActor(actor);
    }

    private void checkGlError(GL gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );  
    }

	@Override
	public void display(GLAutoDrawable glDrawable) 
	{
	    GL gl = glDrawable.getGL();
	    checkGlError(gl, "GLSceneComposer display 123");
	    GLActorContext actorContext = new GLActorContext(glDrawable, gl2Adapter);
        checkGlError(gl, "GLSceneComposer display 125");
	    if (viewChanged) {
            updateModelViewMatrix(actorContext);
	        viewChanged = false;
	    }
        checkGlError(gl, "GLSceneComposer display 130");
	    if (stereoModeNeedsCleanup) {
	        // On Mountain Lion, GL_BACK, GL_BACK_LEFT, and GL_BACK_RIGHT
	        // are separate buffers, all of which display. Clear them all
	        // between stereo mode changes.
	        // Otherwise, unclearable ghost images remain in the background.
	        gl.glClearColor(0,0,0,0);
	        GL2GL3 gl2gl3 = gl.getGL2GL3();
	        GLCapabilitiesImmutable glCaps = glDrawable.getChosenGLCapabilities();
	        checkGlError(gl, "GLSceneComposer display 139");
	        if (glCaps.getStereo())
	        {
	            checkGlError(gl, "GLSceneComposer display 141");
	            if (glCaps.getDoubleBuffered()) {
                    gl2gl3.glDrawBuffer(GL2GL3.GL_BACK_RIGHT);
                    gl.glClear(GL.GL_COLOR_BUFFER_BIT);
                    gl2gl3.glDrawBuffer(GL2GL3.GL_BACK_LEFT);
                    checkGlError(gl, "GLSceneComposer display 146");
                    gl.glClear(GL.GL_COLOR_BUFFER_BIT);
                    gl2gl3.glDrawBuffer(GL2GL3.GL_BACK);
                    gl.glClear(GL.GL_COLOR_BUFFER_BIT);
                    checkGlError(gl, "GLSceneComposer display 151");
	            }
                checkGlError(gl, "GLSceneComposer display 167");
    	        checkGlError(gl, "GLSceneComposer display 146");
                gl2gl3.glDrawBuffer(GL2GL3.GL_BACK);
    	        checkGlError(gl, "GLSceneComposer display 148");
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);
	        }
	        stereoModeNeedsCleanup = false;
	    }
        checkGlError(gl, "GLSceneComposer display 152");
	    stereoMode.display(actorContext, this);
        checkGlError(gl, "GLSceneComposer display 154");
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
        GL gl = actorContext.getGLAutoDrawable().getGL();
        GLError.checkGlError(gl, "GLSceneComposer displayScene 170");
        if (useDepth) {
            gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        }
        // Render in 4 passes
        GLError.checkGlError(gl, "GLSceneComposer displayScene 175");
        gl.glDisable(GL.GL_DEPTH_TEST);
        GLError.checkGlError(gl, "GLSceneComposer displayScene 177");
        displayBackground(actorContext);
        if (useDepth)
            gl.glEnable(GL.GL_DEPTH_TEST);        	
        displayOpaque(actorContext);
        displayTransparent(actorContext);
        displayHud(actorContext);
    }
    
	@Override
	public void dispose(GLAutoDrawable glDrawable) {
        GLActorContext actorContext = new GLActorContext(glDrawable, gl2Adapter);
	    for (GL3Actor actor : allActors)
	        actor.dispose(actorContext);
	    stereoMode.dispose(glDrawable);
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
	    final GL gl = glDrawable.getGL();
        checkGlError(gl, "GLSceneComposer init 204");
	    GL2GL3 gl2gl3 = gl.getGL2GL3();
	    // Use sRGB framebuffer for correct lighting on computer screens
        // Why does GL_FRAMEBUFFER_SRGB work here, but not in slice viewer?
	    // Could be GLCanvas vs GLJPanel...
	    gl2gl3.glEnable(GL2GL3.GL_FRAMEBUFFER_SRGB);
	    // 
	    gl2Adapter = GL2AdapterFactory.createGL2Adapter(glDrawable);
	    GLActorContext actorContext = new GLActorContext(glDrawable, gl2Adapter);
		if (useDepth) {
			gl.glEnable(GL.GL_DEPTH_TEST);
			gl.glClearDepth(1.0);
		}
		for (GL3Actor actor : allActors)
		    actor.init(actorContext);
        checkGlError(gl, "GLSceneComposer init 217");
	}

	@Override
	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width,
			int height) 
	{
        final GL gl = glDrawable.getGL();
        checkGlError(gl, "GLSceneComposer reshape 223");
	    stereoMode.reshape(glDrawable, x, y, width, height);
		viewChanged = true;
        checkGlError(gl, "GLSceneComposer reshape 228");
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

	public void setStereoMode(StereoMode mode) {
		// if (this.stereoMode == mode) return; // Just swap eye might have changed, still repaint
	    if (this.stereoMode != mode) {
	        stereoMode = mode;
	        stereoModeNeedsCleanup = true;
	        mode.reshape(glComponent.getWidth(), glComponent.getHeight());
	    }
		viewChangedSignal.emit();
	}

}
