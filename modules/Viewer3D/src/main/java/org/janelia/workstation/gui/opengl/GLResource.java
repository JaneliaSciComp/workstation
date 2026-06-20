package org.janelia.workstation.gui.opengl;

// import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

public interface GLResource {
	public void init(GLAutoDrawable glDrawable);
	public void dispose(GLAutoDrawable glDrawable);
}
