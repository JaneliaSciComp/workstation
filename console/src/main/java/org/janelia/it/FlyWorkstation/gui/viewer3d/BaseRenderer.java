package org.janelia.it.FlyWorkstation.gui.viewer3d;

import java.awt.Color;
import java.util.Vector;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

public abstract class BaseRenderer implements GLEventListener
{
    protected GLU glu = new GLU();
    protected Vector<GLActor> actors = new Vector<GLActor>();
    protected Color backgroundColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);

    public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public void addActor(GLActor actor) {
		actors.add(actor);
    }

    @Override
    public void display(GLAutoDrawable gLDrawable) 
    {
        final GL2 gl = gLDrawable.getGL().getGL2();
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
        final GL2 gl = glDrawable.getGL().getGL2();
		for (GLActor actor : actors)
			actor.dispose(gl);
	}

    @Override
    public void init(GLAutoDrawable gLDrawable) 
    {
    		// System.out.println("init() called");
        GL2 gl = gLDrawable.getGL().getGL2();
        gl.glEnable(GL2.GL_FRAMEBUFFER_SRGB);
		for (GLActor actor : actors)
			actor.init(gl);
    }
}
