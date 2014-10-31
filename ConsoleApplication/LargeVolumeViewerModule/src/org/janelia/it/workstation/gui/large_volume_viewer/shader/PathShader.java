package org.janelia.it.workstation.gui.large_volume_viewer.shader;

public class PathShader extends PassThroughTextureShader {

	@Override
	public String getFragmentShader() {
		return "PathFrag.glsl";
	}
	
	@Override
	public String getVertexShader() {
		return "PathVrtx.glsl";
	}
}
