package org.janelia.it.workstation.gui.geometric_search.gl;

/**
 * Created by murphys on 5/15/15.
 */
public class OITMeshSortShader extends GL4Shader {

    @Override
    public String getVertexShaderResourceName() {
        return "OITMeshSortVertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "OITMeshSortFragment.glsl";
    }
}
