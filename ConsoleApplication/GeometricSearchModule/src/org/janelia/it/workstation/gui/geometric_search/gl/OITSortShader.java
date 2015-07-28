package org.janelia.it.workstation.gui.geometric_search.gl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by murphys on 5/15/15.
 */
public class OITSortShader extends GL4Shader {

    private final Logger logger = LoggerFactory.getLogger(OITSortShader.class);

    private GL4TransparencyContext tc;
    
    public void setTransparencyContext(GL4TransparencyContext tc) {
        this.tc=tc;
    }

    @Override
    public String getVertexShaderResourceName() {
        return "OITSortVertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "OITSortFragment.glsl";
    }

    IntBuffer quadDataBufferId=IntBuffer.allocate(1);
    FloatBuffer quadFb;
    IntBuffer vertexArrayId=IntBuffer.allocate(1);


    @Override
    public void display(GL4 gl) {
        super.display(gl);
        checkGlError(gl, "d1 super.display() error");

//        int uniformLoc1 = gl.glGetUniformLocation(getShaderProgram(), "head_pointer_image");
//        checkGlError(gl, "d2 glGetUniformLocation() error");
//
//        if (uniformLoc1<0) {
//            logger.error("uniformLoc1 less than 0");
//        }
//        gl.glUniform1i(uniformLoc1, tc.getHeadPointerTextureId());
//        checkGlError(gl, "d3 glUniform1i() error");
//
//
//        int uniformLoc2 = gl.glGetUniformLocation(getShaderProgram(), "list_buffer");
//        checkGlError(gl, "d4 glGetUniformLocation() error");
//
//        if (uniformLoc2<0) {
//            logger.error("uniformLoc2 less than 0");
//        }
//        gl.glUniform1i(uniformLoc2, tc.getFragmentStorageBufferId());
//        checkGlError(gl, "d5 glUniform1i() error");


//        gl.glBindTexture(GL4.GL_TEXTURE_2D, tc.getHeadPointerTextureId());
//        checkGlError(gl, "d6 OITSortShader glBindTexture() error");
//        
//        gl.glBindBuffer(GL4.GL_TEXTURE_BUFFER, tc.getFragmentStorageBufferId());
//        checkGlError(gl, "d 6.1 OITSortShader glBindBufferBase() error");
        
       // Bind the headPointerTexture for read-write
       gl.glBindImageTexture(1, tc.getHeadPointerTextureId(), 0, false, 0, GL4.GL_READ_WRITE, GL4.GL_R32UI);
       checkGlError(gl, "d6.2 OITSortShader glBindImageTexture() error");
       
       // Bind the fragment list texture for read-write
//       gl.glBindImageTexture(0, tc.getFragmentStorageTextureId(), 0, false, 0, GL4.GL_READ_WRITE, GL4.GL_RGBA32UI);
//       checkGlError(gl, "d6.3 OITSortShader glBindImageTexture() error");
                
        // Bind and reset the atomic counter       
//        gl.glBindBuffer(GL4.GL_ATOMIC_COUNTER_BUFFER, tc.getAtomicCounterId());
//        checkGlError(gl, "d6.4 OITSortShader glBindBuffer() error");

        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d7 OITSortShader glBindVertexArray() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, quadDataBufferId.get(0));
        checkGlError(gl, "d8 OITSortShader glBindBuffer error");

        gl.glVertexAttribPointer(0, 4, GL4.GL_FLOAT, false, 0, 0);
        checkGlError(gl, "d9 OITSortShader glVertexAttribPointer 0 () error");

        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d10 OITSortShader glEnableVertexAttribArray 0 () error");

        gl.glVertexAttribPointer(1, 2, GL4.GL_FLOAT, false, 0, 16 * 4);
        checkGlError(gl, "d11 OITSortShader glVertexAttribPointer 1 () error");

        gl.glEnableVertexAttribArray(1);
        checkGlError(gl, "d12 OITSortShader glEnableVertexAttribArray 1 () error");

        gl.glDrawArrays(GL4.GL_TRIANGLE_FAN, 0, 4);
        checkGlError(gl, "d13 OITSortShader glDrawArrays() error");

    }

    @Override
    public void init(GL4 gl) throws ShaderCreationException {
        super.init(gl);
        gl.glUseProgram(getShaderProgram());

     //   gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId.get(0));
     //   checkGlError(gl, "i glBindTexture() error");

//        gl.glTexStorage2D(GL4.GL_TEXTURE_2D,
//                1,
//                GL4.GL_RGBA32F,
//                4, 4);
//        checkGlError(gl, "i glTexStorage2D() error");

//        gl.glTexSubImage2D(GL4.GL_TEXTURE_2D,
//                0,
//                0, 0,
//                4, 4,
//                GL4.GL_RGBA,
//                GL4.GL_FLOAT,
//                textureFb);
//        checkGlError(gl, "i glTexSubImage error");


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
