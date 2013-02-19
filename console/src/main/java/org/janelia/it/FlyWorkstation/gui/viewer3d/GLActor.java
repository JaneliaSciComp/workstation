package org.janelia.it.FlyWorkstation.gui.viewer3d;

import javax.media.opengl.GL2;

public interface GLActor {
	/**
	 * Renders this actor in the given OpenGL context
	 * @param gl
	 */
	public void display(GL2 gl);
	
	public BoundingBox3d getBoundingBox3d();

	/**
	 * Initializes this actor for the given OpenGL context
	 * @param gl
	 */
	public void init(GL2 gl);
	
	/**
	 * Disposes of resources allocated for this actor.
	 * @param gl
	 */
	public void dispose(GL2 gl);
}
