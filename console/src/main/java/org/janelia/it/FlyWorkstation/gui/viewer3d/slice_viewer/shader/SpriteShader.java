package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader;

import javax.media.opengl.GL2;

public class SpriteShader extends PassThroughTextureShader {

	@Override
	public String getFragmentShader() {
		return "SpriteFrag.glsl";
	}
	
	@Override
	public String getVertexShader() {
		return "SpriteVrtx.glsl";
	}
	
	@Override
	public void load(GL2 gl) {
		// System.out.println("OutlineShader load");
		super.load(gl);
		// checkGlError(gl, "OutlineShader load");
	}
}
