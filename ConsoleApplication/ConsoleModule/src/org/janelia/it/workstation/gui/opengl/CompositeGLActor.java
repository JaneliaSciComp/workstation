package org.janelia.it.workstation.gui.opengl;

import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;

import java.util.List;
import java.util.Vector;

import javax.media.opengl.GL;


public class CompositeGLActor 
implements GL3Actor
{
    protected List<GL3Actor> actors = new Vector<GL3Actor>();

	public void addActor(GL3Actor actor) {
		actors.add(actor);
	}
	
    @Override
    public BoundingBox3d getBoundingBox3d() {
        BoundingBox3d result = new BoundingBox3d();
        for (GL3Actor actor : actors)
            result.include(actor.getBoundingBox3d());
        return result;
    }

    @Override
    public void display(GLActorContext context) {
    	GL gl = context.getGLAutoDrawable().getGL();
        for (GL3Actor actor : actors) {
        	GLError.checkGlError(gl, "CompositeGLActor display 32");
            actor.display(context);        
        	GLError.checkGlError(gl, "CompositeGLActor display 34");
        }
    }

    @Override
    public void init(GLActorContext context) {
    	GL gl = context.getGLAutoDrawable().getGL();
        for (GL3Actor actor : actors) {
            GLError.checkGlError(gl, "CompositeGLActor init 42");
            actor.init(context);        
            GLError.checkGlError(gl, "CompositeGLActor init 44");
        }
    }

    @Override
    public void dispose(GLActorContext context) {
        for (GL3Actor actor : actors)
            actor.dispose(context);        
    }
}
