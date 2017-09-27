package org.janelia.it.workstation.ab2.shader;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;

public class AB2ActorPickShader extends GLShaderProgram {
    @Override
    public String getVertexShaderResourceName() {
        return null;
    }

    @Override
    public String getFragmentShaderResourceName() {
        return null;
    }

    public void setMVP(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp", false, mvp.asArray());
        checkGlError(gl, "AB2ActorShader setMVP() error");
    }

    public void setPickId(GL4 gl, int pickId) {
        setUniform(gl, "pickId", pickId);
        checkGlError(gl, "AB2ActorShader setPickId() error");
    }

    public void setTwoDimensional(GL4 gl, boolean twoDimensional) {
        if (twoDimensional) {
            setUniform(gl, "twoDimensional", 1);
        } else {
            setUniform(gl, "twoDimensional", 0);
        }
        checkGlError(gl, "AB2ActorShader setTwoDimensional() error");
    }



}
