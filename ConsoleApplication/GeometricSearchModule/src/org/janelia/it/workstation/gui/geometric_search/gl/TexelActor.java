package org.janelia.it.workstation.gui.geometric_search.gl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by murphys on 5/14/15.
 */
public class TexelActor extends GL4SimpleActor {

    private final Logger logger = LoggerFactory.getLogger(TexelActor.class);

    IntBuffer textureId=IntBuffer.allocate(1);
    FloatBuffer textureFb;
    IntBuffer quadDataBufferId=IntBuffer.allocate(1);
    IntBuffer quadVertexBufferId=IntBuffer.allocate(1);
    FloatBuffer quadFb;
    IntBuffer vertexArrayId=IntBuffer.allocate(1);


    @Override
    public void display(GL4 gl) {
        super.display(gl);
        checkGlError(gl, "d super.display() error");

        gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId.get(0));
        checkGlError(gl, "d glBindTexture() error");

        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d glBindVertexArray() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, quadDataBufferId.get(0));
        checkGlError(gl, "d glBindBuffer error");

        gl.glVertexAttribPointer(0, 4, GL4.GL_FLOAT, false, 0, 0);
        checkGlError(gl, "d glVertexAttribPointer 0 () error");

        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d glEnableVertexAttribArray 0 () error");

        gl.glVertexAttribPointer(1, 2, GL4.GL_FLOAT, false, 0, 16 * 4);
        checkGlError(gl, "d glVertexAttribPointer 1 () error");

        gl.glEnableVertexAttribArray(1);
        checkGlError(gl, "d glEnableVertexAttribArray 1 () error");

        gl.glDrawArrays(GL4.GL_TRIANGLE_FAN, 0, 4);
        checkGlError(gl, "d glDrawArrays() error");

    }

    @Override
    public void init(GL4 gl) {

        // TEXTURE ////////////////////////

        textureFb = FloatBuffer.allocate(16*4);

        for (int i=0;i<4;i++) {
            for (int j=0;j<4;j++) {
                int p=(i*4+j)*4;
                if ( (j & 1) == 0 ) { // if j even
                    if ((i & 1) == 0) { // if i even
                        // white
                        textureFb.put(p, 1.0f);
                        textureFb.put(p+1, 0.2f);
                        textureFb.put(p+2, 0.2f);
                        textureFb.put(p+3, 1.0f);
                    } else {
                        // black
                        textureFb.put(p, 0.0f);
                        textureFb.put(p+1, 1.0f);
                        textureFb.put(p+2, 0.0f);
                        textureFb.put(p+3, 1.0f);
                    }
                } else { // j is odd
                    if ((i & 1) == 0) { // if i even
                        // black
                        textureFb.put(p, 0.0f);
                        textureFb.put(p+1, 1.0f);
                        textureFb.put(p+2, 0.0f);
                        textureFb.put(p+3, 1.0f);
                    } else {
                        // white
                        textureFb.put(p, 1.0f);
                        textureFb.put(p+1, 0.2f);
                        textureFb.put(p+2, 0.2f);
                        textureFb.put(p+3, 1.0f);
                    }
                }

            }
        }

        gl.glGenTextures(1, textureId);
        checkGlError(gl, "i glGenTextures() error");

        gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId.get(0));
        checkGlError(gl, "i glBindTexture() error");

        gl.glTexStorage2D(GL4.GL_TEXTURE_2D,
                1,
                GL4.GL_RGBA32F,
                4, 4);
        checkGlError(gl, "i glTexStorage2D() error");

        gl.glTexSubImage2D(GL4.GL_TEXTURE_2D,
                0,
                0, 0,
                4, 4,
                GL4.GL_RGBA,
                GL4.GL_FLOAT,
                textureFb);
        checkGlError(gl, "i glTexSubImage error");


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
        checkGlError(gl, "i glGenVertexArrays error");

        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "i glBindVertexArray error");

        gl.glGenBuffers(1, quadDataBufferId);
        checkGlError(gl, "i glGenBuffers() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, quadDataBufferId.get(0));
        checkGlError(gl, "i glBindBuffer error");

        gl.glBufferData(GL4.GL_ARRAY_BUFFER, quadFb.capacity() * 4, quadFb, GL4.GL_STATIC_DRAW);
        checkGlError(gl, "d glBufferData error");

    }

    @Override
    public void dispose(GL4 gl) {

    }


}
