package org.janelia.it.workstation.ab2.shader;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;

public class AB2Volume3DShader extends GLShaderProgram {

    public static final int TEXTURE_TYPE_NONE   =0;
    public static final int TEXTURE_TYPE_2D_RGBA=1;
    public static final int TEXTURE_TYPE_2D_R8  =2;
    public static final int TEXTURE_TYPE_3D_RGBA=3;

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

}
