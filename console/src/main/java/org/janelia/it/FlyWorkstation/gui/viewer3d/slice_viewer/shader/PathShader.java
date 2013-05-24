package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader;

import javax.media.opengl.GL2;

public class PathShader extends PassThroughTextureShader {

	@Override
	public String getFragmentShader() {
		return "PathFrag.glsl";
	}
	
	@Override
	public String getVertexShader() {
		return "PathVrtx.glsl";
	}
	
	@Override
	public void load(GL2 gl) {
		// System.out.println("PathShader load");
		super.load(gl);
		// checkGlError(gl, "PathShader load");
	}
}
