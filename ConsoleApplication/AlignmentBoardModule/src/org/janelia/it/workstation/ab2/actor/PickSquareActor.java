package org.janelia.it.workstation.ab2.actor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector2;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2PickSquareColorChangeEvent;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer2D;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer3D;
import org.janelia.it.workstation.ab2.shader.AB2Basic2DShader;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PickSquareActor extends GLAbstractActor {

    private final Logger logger = LoggerFactory.getLogger(PickSquareActor.class);

    Vector3 v0;
    Vector3 v1;
    Vector4 color0;
    Vector4 color1;

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);

    FloatBuffer vertexFb;
    AB2Renderer2D renderer2d;

    public PickSquareActor(AB2Renderer2D renderer, int actorId, Vector3 v0, Vector3 v1, Vector4 color0, Vector4 color1) {
        super(renderer, actorId);
        this.renderer2d=renderer;
        this.v0=v0;
        this.v1=v1;
        this.color0=color0;
        this.color1=color1;
    }

    @Override
    public boolean isTwoDimensional() { return true; }

    @Override
    public void init(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Basic2DShader) {

            float[] vertexData = {

                    v0.get(0), v0.get(1), v0.get(2),
                    v1.get(0), v0.get(1), v0.get(2),
                    v0.get(0), v1.get(1), v0.get(2),

                    v1.get(0), v0.get(1), v1.get(2),
                    v1.get(0), v1.get(1), v1.get(2),
                    v0.get(0), v1.get(1), v1.get(2)
            };

            vertexFb=createGLFloatBuffer(vertexData);

            gl.glGenVertexArrays(1, vertexArrayId);
            checkGlError(gl, "i1 glGenVertexArrays error");

            gl.glBindVertexArray(vertexArrayId.get(0));
            checkGlError(gl, "i2 glBindVertexArray error");

            gl.glGenBuffers(1, vertexBufferId);
            checkGlError(gl, "i3 glGenBuffers() error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            checkGlError(gl, "i4 glBindBuffer error");

            gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertexFb.capacity() * 4, vertexFb, GL4.GL_STATIC_DRAW);
            checkGlError(gl, "i5 glBufferData error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        }

        // Deprecated design, in favor of the actor taking responsibility based on setSelect(), etc.

//        else if (shader instanceof AB2PickShader) {
//            AB2Controller.getController().setPickEvent(actorId, new AB2PickSquareColorChangeEvent(this));
//        }

    }

    @Override
    public void display(GL4 gl, GLShaderProgram shader) {

        if (shader instanceof AB2Basic2DShader) {
            AB2Basic2DShader basic2DShader=(AB2Basic2DShader)shader;
            basic2DShader.setMVP2d(gl, getModelMatrix().multiply(renderer2d.getVp2d()));
            Vector4 actorColor=renderer.getColorIdMap().get(actorId);
            if (actorColor!=null) {
                basic2DShader.setColor(gl, actorColor);
            }

        } else if (shader instanceof AB2PickShader) {
            AB2PickShader pickShader=(AB2PickShader)shader;
            pickShader.setMVP(gl, getModelMatrix().multiply(renderer2d.getVp2d()));
            pickShader.setPickId(gl, actorId);
        }

        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d1 glBindVertexArray() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "d2 glBindBuffer error");

        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        checkGlError(gl, "d3 glVertexAttribPointer 0 () error");

        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d4 glEnableVertexAttribArray 0 () error");

        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexFb.capacity()/2);
        checkGlError(gl, "d7 glDrawArrays() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Basic2DShader) {
            gl.glDeleteVertexArrays(1, vertexArrayId);
            gl.glDeleteBuffers(1, vertexBufferId);
        }
        super.dispose(gl, shader);
    }

    public Vector4 getColor0() { return color0; }

    public Vector4 getColor1() { return color1; }

}
