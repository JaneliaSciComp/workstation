package org.janelia.it.workstation.ab2.shader;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;

public class AB2Basic3DShader extends GLShaderProgram {
    @Override
    public String getVertexShaderResourceName() { return "AB2Basic3DShader_vertex.glsl"; }

    @Override
    public String getFragmentShaderResourceName() {
        return "AB2Basic3DShader_fragment.glsl";
    }

    public void setMVP(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp", false, mvp.asArray());
        checkGlError(gl, "AB2Basic3DShader setMVP() error");
    }
}
