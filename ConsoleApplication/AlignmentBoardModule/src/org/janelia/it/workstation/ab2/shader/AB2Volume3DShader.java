package org.janelia.it.workstation.ab2.shader;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;

public class AB2Volume3DShader extends GLShaderProgram {

    @Override
    public String getVertexShaderResourceName() {
        return "AB2Volume3DShader_vertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "AB2Volume3DShader_fragment.glsl";
    }

    public void setMVP3d(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp3d", false, mvp.asArray());
        checkGlError(gl, "AB2Volume3DShader setMVP3d() error");
    }

    public void setTextureMVP3d(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "textureMvp3d", false, mvp.asArray());
        checkGlError(gl, "AB2Volume3DShader setTextureMVP3d() error");
    }

    public void setImageDim(GL4 gl, Vector3 dim) {
        setUniform3v(gl, "image_dim", 1, dim.toArray());
        checkGlError(gl, "AB2Volume3DShader setImageDim() error");
    }

    public void setImageMaxDIm(GL4 gl, float maxDim) {
        setUniform(gl, "image_max_dim", maxDim);
        checkGlError(gl, "AB2Volume3DShader setImageMaxDim() error");
    }

}
