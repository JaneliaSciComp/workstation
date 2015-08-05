package org.janelia.it.workstation.gui.geometric_search.viewer.gl;

/**
 * Created by murphys on 5/14/15.
 */
public class TexelShader extends GL4Shader {

    @Override
    public String getVertexShaderResourceName() {
        return "TexelVertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "TexelFragment.glsl";
    }
}
