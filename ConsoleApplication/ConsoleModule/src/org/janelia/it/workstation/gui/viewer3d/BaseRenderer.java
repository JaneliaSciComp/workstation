package org.janelia.it.workstation.gui.viewer3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL2;
// import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.opengl.GLActor;

// Shared base class of MipRenderer and SliceRenderer
public abstract class BaseRenderer implements GLEventListener
{
    public static final double DISTANCE_TO_SCREEN_IN_PIXELS = 2000;

    protected GLU glu = new GLU();
    protected List<GLActor> actors = new ArrayList<GLActor>();
	protected Color backgroundColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    protected Camera3d camera;

	public void addActor(GLActor actor) {
		actors.add(actor);
    }

    @Override
    public void display(GLAutoDrawable gLDrawable) 
    {
        final GL2 gl = gLDrawable.getGL().getGL2();
	    displayBackground(gl);
    }
 
    protected void displayBackground(GL2 gl) 
    {
        // paint solid background color
	    gl.glClearColor(
	    		backgroundColor.getRed()/255.0f,
	    		backgroundColor.getGreen()/255.0f,
	    		backgroundColor.getBlue()/255.0f,
	    		backgroundColor.getAlpha()/255.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);    		
    }

    public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) 
    {
    		// System.out.println("displayChanged called");
    }
    
    @Override
	public void dispose(GLAutoDrawable glDrawable) 
	{
		for (GLActor actor : actors)
			actor.dispose(glDrawable);
	}

    public List<GLActor> getActors() {
		return actors;
	}

    public Color getBackgroundColor() {
		return backgroundColor;
	}

    public Camera3d getCamera() {
		return camera;
	}

    @Override
    public void init(GLAutoDrawable gLDrawable)
    {
    	// System.out.println("init() called");
        // GL2GL3 gl2gl3 = gLDrawable.getGL().getGL2GL3();
        // Because of trouble with JOGL2.1/GLJPanel/GL_FRAMEBUFFER_SRGB, 
        // need to correct for srgb in framebuffer, not here.
        // gl2gl3.glEnable(GL2.GL_FRAMEBUFFER_SRGB); // srgb correct in shader...
        List<GLActor> localActors = new ArrayList<GLActor>( getActors() );
		for (GLActor actor : localActors) {
            actor.init(gLDrawable);
        }
    }

    public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

    public void setCamera(Camera3d camera) {
		this.camera = camera;
	}
}
