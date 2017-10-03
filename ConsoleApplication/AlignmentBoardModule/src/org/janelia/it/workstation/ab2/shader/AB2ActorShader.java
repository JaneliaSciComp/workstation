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

    public void setMVP3d(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp3d", false, mvp.asArray());
        checkGlError(gl, "AB2ActorShader setMVP3d() error");
    }

    public void setMVP2d(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp2d", false, mvp.asArray());
        checkGlError(gl, "AB2ActorShader setMVP2d() error");
    }

    public void setColor0(GL4 gl, Vector4 color0) {
        setUniform4v(gl, "color0", 1, color0.toArray());
        checkGlError(gl, "AB2ActorShader setColor0() error");
    }

    public void setColor1(GL4 gl, Vector4 color1) {
        setUniform4v(gl, "color1", 1, color1.toArray());
        checkGlError(gl, "AB2ActorShader setColor1() error");
    }

    public void setTwoDimensional(GL4 gl, boolean twoDimensional) {
        if (twoDimensional) {
            setUniform(gl, "twoDimensional", 1);
        } else {
            setUniform(gl, "twoDimensional", 0);
        }
        checkGlError(gl, "AB2ActorShader setTwoDimensional() error");
    }

    public void setApplyImageRGBATexture(GL4 gl, boolean applyImageTexture) {
        if (applyImageTexture) {
            setUniform(gl, "applyImageRGBATexture", 1);
        } else {
            setUniform(gl, "applyImageRGBATexture", 0);
        }
        checkGlError(gl, "AB2ActorShader setApplyImageRGBTexture() error");
    }

    public void setApplyImageR8Texture(GL4 gl, boolean applyImageTexture) {
        if (applyImageTexture) {
            setUniform(gl, "applyImageR8Texture", 1);
        } else {
            setUniform(gl, "applyImageR8Texture", 0);
        }
        checkGlError(gl, "AB2ActorShader setApplyImageR8Texture() error");
    }


}
