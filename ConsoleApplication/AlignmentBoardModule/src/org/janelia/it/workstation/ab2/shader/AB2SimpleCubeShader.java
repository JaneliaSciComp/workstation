package org.janelia.it.workstation.ab2.shader;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;

public class AB2SimpleCubeShader extends GLShaderProgram {

    @Override
    public String getVertexShaderResourceName() { return "AB2SimpleCubeShader_vertex.glsl"; }

    @Override
    public String getFragmentShaderResourceName() {
        return "AB2SimpleCubeShader_fragment.glsl";
    }

    public void setMVP(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp", false, mvp.asArray());
        checkGlError(gl, "AB2SimpleCubeShader setMVP() error");
    }
}
