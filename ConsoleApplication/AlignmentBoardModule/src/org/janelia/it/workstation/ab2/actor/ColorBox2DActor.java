package org.janelia.it.workstation.ab2.actor;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector2;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer2D;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer3D;
import org.janelia.it.workstation.ab2.shader.AB2Basic2DShader;
import org.janelia.it.workstation.ab2.shader.AB2Image2DShader;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//todo: finish rewriting this as ColorBox2DActor from Image2DActor
public class ColorBox2DActor extends GLAbstractActor {

    private final Logger logger = LoggerFactory.getLogger(ColorBox2DActor.class);

    Vector2 v0;
    Vector2 v1;

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    FloatBuffer vertexFb;

//    IntBuffer imageTextureId=IntBuffer.allocate(1);
//    BufferedImage bufferedImage;
//    float alpha;

    AB2Renderer2D renderer2d;

    public ColorBox2DActor(AB2Renderer2D renderer, int actorId, Vector2 v0, Vector2 v1) {
        super(renderer);
        this.renderer2d=renderer;
        this.actorId=actorId;
        this.v0=v0;
        this.v1=v1;
    }

    @Override
    public boolean isTwoDimensional() { return true; }

    @Override
    public void init(GL4 gl, GLShaderProgram shader) {

        if (shader instanceof AB2Image2DShader) {

            AB2Basic2DShader basic2DShader=(AB2Basic2DShader)shader;

            // This combines positional vertices interleaved with 2D texture coordinates. Note: z not used
            // but necessary for shader compatibility.
            float[] vertexData = {

                    v0.get(0), v0.get(1), 0f,    // lower left
                    v1.get(0), v0.get(1), 0f,    // lower right
                    v0.get(0), v1.get(1), 0f,    // upper left

                    v1.get(0), v0.get(1), 0f,    // lower right
                    v1.get(0), v1.get(1), 0f,    // upper right
                    v0.get(0), v1.get(1), 0f     // upper left
            };

            vertexFb=createGLFloatBuffer(vertexData);

            gl.glGenVertexArrays(1, vertexArrayId);
            gl.glBindVertexArray(vertexArrayId.get(0));
            gl.glGenBuffers(1, vertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertexFb.capacity() * 4, vertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        } else if (shader instanceof AB2PickShader) {

            if (pickIndex<0) {
                pickIndex = AB2Controller.getController().getNextPickIndex();
            }
        }

    }

    @Override
    public void display(GL4 gl, GLShaderProgram shader) {

        if (shader instanceof AB2Basic2DShader) {
            AB2Basic2DShader basic2DShader=(AB2Basic2DShader)shader;
            basic2DShader.setMVP2d(gl, getModelMatrix().multiply(renderer2d.getVp2d()));
        } else if (shader instanceof AB2PickShader) {
            AB2PickShader pickShader=(AB2PickShader)shader;
            pickShader.setMVP(gl, renderer2d.getVp2d());
            pickShader.setPickId(gl, getPickIndex());
        }

        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d3 glBindVertexArray()");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "d4 glBindBuffer()");

        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        checkGlError(gl, "d5 glVertexAttribPointer()");

        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d6 glEnableVertexAttribArray()");

        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexFb.capacity()/2);
        checkGlError(gl, "d9 glDrawArrays()");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        checkGlError(gl, "d10 glBindBuffer()");

    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Basic2DShader) {
            gl.glDeleteVertexArrays(1, vertexArrayId);
            gl.glDeleteBuffers(1, vertexBufferId);
        }
        super.dispose(gl, shader);
    }

}
