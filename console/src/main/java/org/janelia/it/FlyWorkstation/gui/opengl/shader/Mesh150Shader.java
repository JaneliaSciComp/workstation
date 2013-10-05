package org.janelia.it.FlyWorkstation.gui.opengl.shader;

public class Mesh150Shader extends BasicShader {

    @Override
    public String getVertexShaderResourceName() {
        return "Mesh150Vrtx.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "Mesh150Frag.glsl";
    }

}
