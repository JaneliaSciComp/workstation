package org.janelia.it.FlyWorkstation.gui.slice_viewer.shader;

import javax.media.opengl.GL2;

public class AnchorShader extends PassThroughTextureShader {

	@Override
	public String getFragmentShader() {
		return "AnchorFrag.glsl";
	}
	
	@Override
	public String getVertexShader() {
		return "AnchorVrtx.glsl";
	}
	
	@Override
	public void load(GL2 gl) {
		// System.out.println("OutlineShader load");
		super.load(gl);
		// checkGlError(gl, "OutlineShader load");
	}
}
