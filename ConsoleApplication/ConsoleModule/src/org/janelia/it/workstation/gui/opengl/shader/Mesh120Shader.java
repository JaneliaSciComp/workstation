package org.janelia.it.workstation.gui.opengl.shader;

/**
 * Mesh shader using GLSL version 1.20 (to work on Mac OS X 10.6)
 * @author brunsc
 *
 */
public class Mesh120Shader extends BasicShader {

    @Override
    public String getVertexShaderResourceName() {
        return "Mesh120Vrtx.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "Mesh120Frag.glsl";
    }

}
