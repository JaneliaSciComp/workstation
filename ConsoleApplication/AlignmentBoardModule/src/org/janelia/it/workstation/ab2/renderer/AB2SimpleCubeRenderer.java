package org.janelia.it.workstation.ab2.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL2;
import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.shader.AB2Basic3DShader;
import org.janelia.it.workstation.ab2.shader.AB2SimpleCubeShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SimpleCubeRenderer extends AB2Basic3DRenderer {

    Logger logger= LoggerFactory.getLogger(AB2SimpleCubeRenderer.class);

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    IntBuffer colorBufferId=IntBuffer.allocate(1);
    IntBuffer interleavedBufferId=IntBuffer.allocate(1);

    FloatBuffer cubeFb;
    FloatBuffer colorFb;
    FloatBuffer interleavedFb;

    boolean useInterleaved=true;


    public AB2SimpleCubeRenderer() {
        super(new AB2SimpleCubeShader());
    }

    @Override
    public void init(GL4 gl) {
        super.init(gl);

        logger.info("init() called");

        //                   X        Y        Z        R        G        B

        float[] interleavedData = { -0.25f,  -0.25f,   0.25f, 0f, 0f, 1f,
                                    -0.25f,   0.25f,   0.25f, 1f, 0f, 0f,
                                     0.25f,   0.25f,   0.25f, 0f, 0f, 1f,
                                     0.25f,  -0.25f,   0.25f, 1f, 0f, 0f,
                                    -0.10f,  -0.10f,   0.75f, 0f, 1f, 0f,
                                    -0.10f,   0.10f,   0.75f, 0f, 1f, 0f,
                                     0.10f,   0.10f,   0.75f, 0f, 1f, 0f,
                                     0.10f,  -0.10f,   0.75f, 0f, 1f, 0f };


        float[] cubeData = { -0.25f,  -0.25f,   0.25f,
                             -0.25f,   0.25f,   0.25f,
                              0.25f,   0.25f,   0.25f,
                              0.25f,  -0.25f,   0.25f,
                             -0.25f,  -0.25f,   0.75f,
                             -0.25f,   0.25f,   0.75f,
                              0.25f,   0.25f,   0.75f,
                              0.25f,  -0.25f,   0.75f };

        float[] colorData = {  1.0f,   0f,     0f,
                               1.0f,   0f,     0f,
                               1.0f,   0f,     0f,
                               1.0f,   0f,     0f,
                               0f,     1.0f,   0f,
                               0f,     1.0f,   0f,
                               0f,     1.0f,   0f,
                               0f,     1.0f,   0f };

        cubeFb= GLAbstractActor.createGLFloatBuffer(cubeData);
        colorFb=GLAbstractActor.createGLFloatBuffer(colorData);
        interleavedFb=GLAbstractActor.createGLFloatBuffer(interleavedData);

        if (useInterleaved) {

            gl.glGenVertexArrays(1, vertexArrayId);
            checkGlError(gl, "i1 glGenVertexArrays error");

            gl.glBindVertexArray(vertexArrayId.get(0));
            checkGlError(gl, "i2 glBindVertexArray error");

            gl.glGenBuffers(1, interleavedBufferId);
            checkGlError(gl, "i3 glGenBuffers() error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, interleavedBufferId.get(0));
            checkGlError(gl, "i4 glBindBuffer error");

            gl.glBufferData(GL4.GL_ARRAY_BUFFER, interleavedFb.capacity() * 4, interleavedFb, GL4.GL_STATIC_DRAW);
            checkGlError(gl, "i5 glBufferData error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        } else {

            gl.glGenVertexArrays(1, vertexArrayId);
            checkGlError(gl, "i1 glGenVertexArrays error");

            gl.glBindVertexArray(vertexArrayId.get(0));
            checkGlError(gl, "i2 glBindVertexArray error");

            gl.glGenBuffers(1, vertexBufferId);
            checkGlError(gl, "i3 glGenBuffers() error");

            gl.glGenBuffers(1, colorBufferId);
            checkGlError(gl, "i3 glGenBuffers() error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            checkGlError(gl, "i4 glBindBuffer error");

            gl.glBufferData(GL4.GL_ARRAY_BUFFER, cubeFb.capacity() * 4, cubeFb, GL4.GL_STATIC_DRAW);
            checkGlError(gl, "i5 glBufferData error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorBufferId.get(0));
            checkGlError(gl, "i4 glBindBuffer error");

            gl.glBufferData(GL4.GL_ARRAY_BUFFER, colorFb.capacity() * 4, colorFb, GL4.GL_STATIC_DRAW);
            checkGlError(gl, "i5 glBufferData error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        }

        logger.info("init() finished");

    }

    @Override
    protected GLDisplayUpdateCallback getDisplayUpdateCallback() {
        return new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {

                //logger.info("update() start");

                if (useInterleaved) {

                    AB2SimpleCubeShader cubeShader = (AB2SimpleCubeShader) shader;
                    cubeShader.setMVP(gl, mvp);
                    gl.glPointSize(3.0f);

                    gl.glBindVertexArray(vertexArrayId.get(0));
                    checkGlError(gl, "d1 ArraySortShader glBindVertexArray() error");

                    gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, interleavedBufferId.get(0));
                    checkGlError(gl, "d2 ArraySortShader glBindBuffer error");

                    gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 24, 0);
                    checkGlError(gl, "d3 ArraySortShader glVertexAttribPointer 0 () error");

                    gl.glEnableVertexAttribArray(0);
                    checkGlError(gl, "d4 ArraySortShader glEnableVertexAttribArray 0 () error");

                    gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 24, 12);
                    checkGlError(gl, "d3 ArraySortShader glVertexAttribPointer 0 () error");

                    gl.glEnableVertexAttribArray(1);
                    checkGlError(gl, "d4 ArraySortShader glEnableVertexAttribArray 0 () error");

                    gl.glDrawArrays(GL4.GL_POINTS, 0, 8);
                    checkGlError(gl, "d7 ArraySortShader glDrawArrays() error");

                    //gl.glDrawArrays(GL4.GL_LINES, 0, 8);
                    //checkGlError(gl, "d8 ArraySortShader glDrawArrays() error");

                    gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

                } else {

                    AB2SimpleCubeShader cubeShader = (AB2SimpleCubeShader) shader;
                    cubeShader.setMVP(gl, mvp);
                    gl.glPointSize(3.0f);

                    gl.glBindVertexArray(vertexArrayId.get(0));
                    checkGlError(gl, "d1 ArraySortShader glBindVertexArray() error");

                    gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
                    checkGlError(gl, "d2 ArraySortShader glBindBuffer error");

                    gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
                    checkGlError(gl, "d3 ArraySortShader glVertexAttribPointer 0 () error");

                    gl.glEnableVertexAttribArray(0);
                    checkGlError(gl, "d4 ArraySortShader glEnableVertexAttribArray 0 () error");

                    gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorBufferId.get(0));
                    checkGlError(gl, "d2 ArraySortShader glBindBuffer error");

                    gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 0);
                    checkGlError(gl, "d3 ArraySortShader glVertexAttribPointer 0 () error");

                    gl.glEnableVertexAttribArray(1);
                    checkGlError(gl, "d4 ArraySortShader glEnableVertexAttribArray 0 () error");

                    gl.glDrawArrays(GL4.GL_POINTS, 0, 8);
                    checkGlError(gl, "d7 ArraySortShader glDrawArrays() error");

                    //gl.glDrawArrays(GL4.GL_LINES, 0, 4);
                    //checkGlError(gl, "d8 ArraySortShader glDrawArrays() error");

                    gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

                }

                //logger.info("update() finish");
            }
        };
    }

}
