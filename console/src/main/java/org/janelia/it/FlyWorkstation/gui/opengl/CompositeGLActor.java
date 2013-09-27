package org.janelia.it.FlyWorkstation.gui.opengl;

import java.util.List;
import java.util.Vector;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;


public class CompositeGLActor 
implements GLActor
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
    public BoundingBox3d getBoundingBox3d() {
        BoundingBox3d result = new BoundingBox3d();
        for (GLActor actor : actors)
            result.include(actor.getBoundingBox3d());
        return result;
    }
}
