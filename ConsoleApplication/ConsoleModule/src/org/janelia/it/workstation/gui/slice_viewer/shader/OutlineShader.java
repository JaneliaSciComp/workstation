package org.janelia.it.workstation.gui.slice_viewer.shader;

import javax.media.opengl.GL2;

public class OutlineShader extends PassThroughTextureShader {

	@Override
	public String getFragmentShader() {
		return "OutlineFrag.glsl";
	}
	
	@Override
	public String getVertexShader() {
		return "OutlineVrtx.glsl";
	}
	
	@Override
	public void load(GL2 gl) {
		// System.out.println("OutlineShader load");
		super.load(gl);
		// checkGlError(gl, "OutlineShader load");
	}
}
