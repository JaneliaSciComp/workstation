package org.janelia.it.FlyWorkstation.gui.opengl;

import java.util.List;
import java.util.Vector;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;


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
        for (GL3Actor actor : actors)
            actor.display(context);        
    }

    @Override
    public void init(GLActorContext context) {
        for (GL3Actor actor : actors)
            actor.init(context);        
    }

    @Override
    public void dispose(GLActorContext context) {
        for (GL3Actor actor : actors)
            actor.dispose(context);        
    }
}
