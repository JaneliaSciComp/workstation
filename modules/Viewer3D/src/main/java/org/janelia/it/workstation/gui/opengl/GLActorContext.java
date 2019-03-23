package org.janelia.it.workstation.gui.opengl;

import javax.media.opengl.GLAutoDrawable;

// TODO - use GLActorContext as the parameter sent to GLActors
public class GLActorContext {
	private GLAutoDrawable glAutoDrawable;
	private GL2Adapter gl2Adapter;

	public GLActorContext (GLAutoDrawable glAutoDrawable, GL2Adapter gl2Adapter) {
		this.glAutoDrawable = glAutoDrawable;
		this.gl2Adapter = gl2Adapter;
	}
	
	public GLAutoDrawable getGLAutoDrawable() {
		return glAutoDrawable;
	}
	
	public GL2Adapter getGL2Adapter() {
		return gl2Adapter;
	}
}
