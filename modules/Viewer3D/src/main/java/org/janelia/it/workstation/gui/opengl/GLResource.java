package org.janelia.it.workstation.gui.opengl;

// import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

public interface GLResource {
	public void init(GLAutoDrawable glDrawable);
	public void dispose(GLAutoDrawable glDrawable);
}
