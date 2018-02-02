package org.janelia.it.workstation.ab2.shader;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2PickShader extends GLShaderProgram {

    Logger logger = LoggerFactory.getLogger(AB2PickShader.class);

    @Override
    public String getVertexShaderResourceName() {
        return "AB2PickShader_vertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "AB2PickShader_fragment.glsl";
    }

    public void setMVP(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp", false, mvp.asArray());
        checkGlError(gl, "AB2PickShader setMVP2d() error");
    }

    public void setPickId(GL4 gl, int pickId) {
        setUniform(gl, "pickId", pickId);
        checkGlError(gl, "AB2PickShader setPickId() error");
    }

}
