package org.janelia.it.FlyWorkstation.gui.opengl.shader;

public class MeshShader extends BasicShader {

    @Override
    public String getVertexShaderResourceName() {
        return "MeshVrtx.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "MeshFrag.glsl";
    }

}
