package org.janelia.it.workstation.ab2.shader;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;

public class AB2Draw2DShader extends GLShaderProgram {

    @Override
    public String getVertexShaderResourceName() {
        return "AB2Draw2DShader_vertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "AB2Draw2DShader_fragment.glsl";
    }

    public void setMVP2d(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp2d", false, mvp.asArray());
        checkGlError(gl, "AB2ActorShader setMVP2d() error");
    }

}
