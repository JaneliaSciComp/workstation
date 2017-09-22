package org.janelia.it.workstation.ab2.shader;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;

public class AB2ActorShader extends GLShaderProgram {

    @Override
    public String getVertexShaderResourceName() {
        return "AB2ActorShader_vertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "AB2ActorShader_fragment.glsl";
    }

    public void setMVP(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp", false, mvp.asArray());
        checkGlError(gl, "AB2ActorShader setMVP() error");
    }

    public void setStyleIdColor(GL4 gl, Vector4 styleIdColor) {
        setUniform4v(gl, "styleIdColor", 1, styleIdColor.toArray());
        checkGlError(gl, "AB2ActorShader setStyleIdColor() error");
    }

}
