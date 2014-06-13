package org.janelia.it.workstation.gui.opengl;

// import javax.media.opengl.GL2;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;

import javax.media.opengl.GLAutoDrawable;

public interface GLActor
extends GLResource
{
	/**
	 * Renders this actor in the given OpenGL context
	 * @param gl
	 */
	public void display(GLAutoDrawable glDrawable);
	
	public BoundingBox3d getBoundingBox3d();

	/**
	 * Initializes this actor for the given OpenGL context
	 * @param gl
	 */
	public void init(GLAutoDrawable glDrawable);
	
	/**
	 * Disposes of resources allocated for this actor.
	 * @param gl
	 */
	public void dispose(GLAutoDrawable glDrawable);
}
