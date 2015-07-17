/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.geometric_search.gl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL4;
import org.janelia.geometry3d.Matrix4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author murphys
 */
public class VolumeShader extends GL4Shader {
    
    private final Logger logger = LoggerFactory.getLogger(VolumeShader.class);

    
    IntBuffer quadDataBufferId=IntBuffer.allocate(1);
    FloatBuffer quadFb;
    IntBuffer vertexArrayId=IntBuffer.allocate(1);
   
    @Override
    public String getVertexShaderResourceName() {
        return "VolumeVertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "VolumeFragment.glsl";
    }
    
    public void setProjection(GL4 gl, Matrix4 projection) {
        setUniformMatrix4fv(gl, "proj", false, projection.asArray());
        checkGlError(gl, "VolumeShader setProjection() error");
    }

    public void setView(GL4 gl, Matrix4 view) {
        setUniformMatrix4fv(gl, "view", false, view.asArray());
        checkGlError(gl, "VolumeShader setView() error");
    }

    public void setModel(GL4 gl, Matrix4 model) {
        setUniformMatrix4fv(gl, "model", false, model.asArray());
        checkGlError(gl, "VolumeShader setModel() error");
    }
    
        @Override
    public void init(GL4 gl) throws ShaderCreationException {
        super.init(gl);
        gl.glUseProgram(getShaderProgram());

        // QUAD ////////////////////////

        quadFb = FloatBuffer.allocate(16 + 8);
        quadFb.put(0, -1.0f); quadFb.put(1, -1.0f); quadFb.put(2, 0.0f); quadFb.put(3, 1.0f);
        quadFb.put(4,  1.0f); quadFb.put(5, -1.0f); quadFb.put(6, 0.0f); quadFb.put(7, 1.0f);
        quadFb.put(8,  1.0f); quadFb.put(9,  1.0f); quadFb.put(10, 0.0f); quadFb.put(11, 1.0f);
        quadFb.put(12, -1.0f); quadFb.put(13, 1.0f); quadFb.put(14, 0.0f); quadFb.put(15, 1.0f);

        quadFb.put(16, 0.0f); quadFb.put(17, 0.0f);
        quadFb.put(18, 1.0f); quadFb.put(19, 0.0f);
        quadFb.put(20, 1.0f); quadFb.put(21, 1.0f);
        quadFb.put(22, 0.0f); quadFb.put(23, 1.0f);

        // VERTEX ARRAY
        gl.glGenVertexArrays(1, vertexArrayId);
        checkGlError(gl, "i1 glGenVertexArrays error");

        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "i2 glBindVertexArray error");

        gl.glGenBuffers(1, quadDataBufferId);
        checkGlError(gl, "i3 glGenBuffers() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, quadDataBufferId.get(0));
        checkGlError(gl, "i4 glBindBuffer error");

        gl.glBufferData(GL4.GL_ARRAY_BUFFER, quadFb.capacity() * 4, quadFb, GL4.GL_STATIC_DRAW);
        checkGlError(gl, "i5 glBufferData error");

    }

    @Override
    public void dispose(GL4 gl) {

    }
    
}
