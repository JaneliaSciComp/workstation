package org.janelia.it.workstation.ab2.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.shader.AB2Basic3DShader;
import org.janelia.it.workstation.ab2.shader.AB2SimpleCubeShader;

public class AB2SimpleCubeRenderer extends AB2Basic3DRenderer {
    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    FloatBuffer cubeFb;


    public AB2SimpleCubeRenderer() {
        super(new AB2SimpleCubeShader());

        cubeFb = FloatBuffer.allocate(8 * 4 + 8 * 3); // location and color

        //                   X        Y        Z        W       R        G        B

        float[] cubeData = { -0.25f,  -0.25f,   0.25f,   1.0f,   0.20f,   0.20f,   0.20f,
                             -0.25f,   0.25f,   0.25f,   1.0f,   0.20f,   1.0f,    0.20f,
                              0.25f,   0.25f,   0.25f,   1.0f,   1.0f,    1.0f,    0.20f,
                              0.25f,  -0.25f,   0.25f,   1.0f,   1.0f,    0.20f,   0.20f,
                             -0.25f,  -0.25f,   0.75f,   1.0f,   0.20f,   0.20f,   1.0f,
                             -0.25f,   0.25f,   0.75f,   1.0f,   0.20f,   1.0f,    1.0f,
                              0.25f,   0.25f,   0.75f,   1.0f,   1.0f,    1.0f,    1.0f,
                              0.25f,  -0.25f,   0.75f,   1.0f,   1.0f,    0.20f,   1.0f  };

        cubeFb.put(cubeData);

    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        super.init(glAutoDrawable);
        final GL4 gl = glAutoDrawable.getGL().getGL4();

        gl.glGenVertexArrays(1, vertexArrayId);
        checkGlError(gl, "i1 glGenVertexArrays error");

        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "i2 glBindVertexArray error");

        gl.glGenBuffers(1, vertexBufferId);
        checkGlError(gl, "i3 glGenBuffers() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "i4 glBindBuffer error");

        gl.glBufferData(GL4.GL_ARRAY_BUFFER, cubeFb.capacity() * 4, cubeFb, GL4.GL_STATIC_DRAW);
        checkGlError(gl, "i5 glBufferData error");
    }

    @Override
    protected GLDisplayUpdateCallback getDisplayUpdateCallback() {
        return new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                AB2SimpleCubeShader cubeShader=(AB2SimpleCubeShader)shader;
                cubeShader.setMVP(gl, mvp);
                gl.glPointSize(3.0f);

                gl.glBindVertexArray(vertexArrayId.get(0));
                checkGlError(gl, "d1 ArraySortShader glBindVertexArray() error");

                gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
                checkGlError(gl, "d2 ArraySortShader glBindBuffer error");

                gl.glVertexAttribPointer(0, 4, GL4.GL_FLOAT, false, 0, 0);
                checkGlError(gl, "d3 ArraySortShader glVertexAttribPointer 0 () error");

                gl.glEnableVertexAttribArray(0);
                checkGlError(gl, "d4 ArraySortShader glEnableVertexAttribArray 0 () error");

                gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 16);
                checkGlError(gl, "d5 ArraySortShader glVertexAttribPointer 1 () error");

                gl.glEnableVertexAttribArray(1);
                checkGlError(gl, "d6 ArraySortShader glEnableVertexAttribArray 1 () error");

                gl.glDrawArrays(GL4.GL_POINTS, 0, 8);
                checkGlError(gl, "d7 ArraySortShader glDrawArrays() error");
            }
        };
    }

}
