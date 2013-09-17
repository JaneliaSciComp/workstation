package org.janelia.it.FlyWorkstation.gui.opengl;

import java.util.List;
import java.util.Vector;
// import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;


public class CompositeGLActor 
implements GLEventListener
{
    private List<GLActor> actors = new Vector<GLActor>();

	public void addActor(GLActor actor) {
		actors.add(actor);
	}
	
	@Override
	public void display(GLAutoDrawable glDrawable) {
        for (GLActor actor : actors)
        	actor.display(glDrawable);
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
        for (GLActor actor : actors)
        	actor.dispose(glDrawable);
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
        for (GLActor actor : actors)
        	actor.init(glDrawable);
	}

	@Override
	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) 
	{}

}
