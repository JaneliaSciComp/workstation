package org.janelia.it.workstation.gui.geometric_search.gl;

/**
 * Created by murphys on 4/28/15.
 */
public class PassthroughShader extends GL3Shader {
    @Override
    public String getVertexShaderResourceName() {
        return "PassthroughVertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "PassthroughFragment.glsl";
    }
}
