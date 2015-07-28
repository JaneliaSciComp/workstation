package org.janelia.it.workstation.gui.geometric_search.gl.oitarr;

import javax.media.opengl.GL4;

/**
 * Created by murphys on 7/25/2015.
 */
public class ArrayMeshShader extends ArrayDrawShader {


    @Override
    public String getVertexShaderResourceName() {
        return "ArrayMeshShader_vertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "ArrayMeshShader_fragment.glsl";
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);
        checkGlError(gl, "d1 ArrayMeshShader super.display() error");
    }
}
