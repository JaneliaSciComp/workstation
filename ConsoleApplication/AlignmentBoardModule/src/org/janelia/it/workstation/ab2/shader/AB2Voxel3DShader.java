package org.janelia.it.workstation.ab2.shader;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;

public class AB2Voxel3DShader extends GLShaderProgram {
    @Override
    public String getVertexShaderResourceName() { return "AB2Voxel3DShader_vertex.glsl"; }

    @Override
    public String getFragmentShaderResourceName() {
        return "AB2Voxel3DShader_fragment.glsl";
    }

    public void setMVP(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp", false, mvp.asArray());
        checkGlError(gl, "AB2Voxel3DShader setMVP() error");
    }

    public void setColor(GL4 gl, Vector4 color) {
        setUniform4v(gl, "color0", 1, color.toArray());
        checkGlError(gl, "AB2Voxel3DShader setColor() error");
    }
}

