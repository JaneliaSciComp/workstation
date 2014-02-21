package org.janelia.it.FlyWorkstation.gui.opengl.shader;

public class SolidColor120Shader extends BasicShader 
{

	@Override
	public String getVertexShaderResourceName() {
		return "SolidColor120Vrtx.glsl";
	}

	@Override
	public String getFragmentShaderResourceName() {
		return "SolidColor120Frag.glsl";
	}

}
