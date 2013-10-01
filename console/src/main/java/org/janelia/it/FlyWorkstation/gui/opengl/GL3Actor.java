package org.janelia.it.FlyWorkstation.gui.opengl;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

/**
 * Extension of GLActor that can make use of GLActor context interface
 * to optionally use faked up OpenGL version < 3.1 state.
 * @author brunsc
 *
 */
public interface GL3Actor {
    /**
     * Renders this actor in the given OpenGL context
     * @param gl
     */
    public void display(GLActorContext context);
    
    public BoundingBox3d getBoundingBox3d();

    /**
     * Initializes this actor for the given OpenGL context
     * @param gl
     */
    public void init(GLActorContext context);
    
    /**
     * Disposes of resources allocated for this actor.
     * @param gl
     */
    public void dispose(GLActorContext context);

}
