package org.janelia.it.FlyWorkstation.gui.opengl;

import java.util.List;
import java.util.Vector;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;

public class CompositeGLActor 
implements GLEventListener
{
    private List<GLActor> actors = new Vector<GLActor>();

	public void addActor(GLActor actor) {
		actors.add(actor);
	}
	
	@Override
	public void display(GLAutoDrawable glDrawable) {
        final GL2 gl = glDrawable.getGL().getGL2();
        for (GLActor actor : actors)
        	actor.display(gl);
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
        final GL2 gl = glDrawable.getGL().getGL2();
        for (GLActor actor : actors)
        	actor.dispose(gl);
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
        final GL2 gl = glDrawable.getGL().getGL2();
        for (GLActor actor : actors)
        	actor.init(gl);
	}

	@Override
	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) 
	{}

}
