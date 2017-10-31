package org.janelia.it.workstation.ab2.actor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.renderer.AB23DRenderer;
import org.janelia.it.workstation.ab2.shader.AB2ActorShader;
import org.janelia.it.workstation.ab2.shader.AB2Basic3DShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoundingBoxActor extends GLAbstractActor
{
    private final Logger logger = LoggerFactory.getLogger(BoundingBoxActor.class);

    Vector3 v0;
    Vector3 v1;

    IntBuffer boundaryVertexArrayId=IntBuffer.allocate(1);
    IntBuffer boundaryVertexBufferId=IntBuffer.allocate(1);

    FloatBuffer boundaryVertexFb;

    public BoundingBoxActor(AB23DRenderer renderer, int actorId, Vector3 v0, Vector3 v1) {
        super(renderer);
        this.actorId=actorId;
        this.v0=v0;
        this.v1=v1;
    }

    @Override
    public void init(GL4 gl, GLShaderProgram shader) {

        if (shader instanceof AB2Basic3DShader) {

            AB2Basic3DShader basic3DShader=(AB2Basic3DShader)shader;

//            logger.info("BoundingBoxActor init() start");

            float[] boundaryData = new float[]{
                    v0.getX(), v0.getY(), v0.getZ(), 0f, 0f, 0f,
                    v0.getX(), v1.getY(), v0.getZ(), 0f, 0f, 0f,

                    v0.getX(), v1.getY(), v0.getZ(), 0f, 0f, 0f,
                    v1.getX(), v1.getY(), v0.getZ(), 0f, 0f, 0f,

                    v1.getX(), v1.getY(), v0.getZ(), 0f, 0f, 0f,
                    v1.getX(), v0.getY(), v0.getZ(), 0f, 0f, 0f,

                    v1.getX(), v0.getY(), v0.getZ(), 0f, 0f, 0f,
                    v0.getX(), v0.getY(), v0.getZ(), 0f, 0f, 0f,

                    v0.getX(), v0.getY(), v1.getZ(), 0f, 0f, 0f,
                    v0.getX(), v1.getY(), v1.getZ(), 0f, 0f, 0f,

                    v0.getX(), v1.getY(), v1.getZ(), 0f, 0f, 0f,
                    v1.getX(), v1.getY(), v1.getZ(), 0f, 0f, 0f,

                    v1.getX(), v1.getY(), v1.getZ(), 0f, 0f, 0f,
                    v1.getX(), v0.getY(), v1.getZ(), 0f, 0f, 0f,

                    v1.getX(), v0.getY(), v1.getZ(), 0f, 0f, 0f,
                    v0.getX(), v0.getY(), v1.getZ(), 0f, 0f, 0f,

                    v0.getX(), v1.getY(), v0.getZ(), 0f, 0f, 0f,
                    v0.getX(), v1.getY(), v1.getZ(), 0f, 0f, 0f,

                    v0.getX(), v0.getY(), v0.getZ(), 0f, 0f, 0f,
                    v0.getX(), v0.getY(), v1.getZ(), 0f, 0f, 0f,

                    v1.getX(), v1.getY(), v0.getZ(), 0f, 0f, 0f,
                    v1.getX(), v1.getY(), v1.getZ(), 0f, 0f, 0f,

                    v1.getX(), v0.getY(), v0.getZ(), 0f, 0f, 0f,
                    v1.getX(), v0.getY(), v1.getZ(), 0f, 0f, 0f
            };

            boundaryVertexFb = createGLFloatBuffer(boundaryData);

//            logger.info("boundaryVertexFb capacity=" + boundaryVertexFb.capacity());

            checkGlError(gl, "i0 pre-glGenVertexArrays error");
            gl.glGenVertexArrays(1, boundaryVertexArrayId);
            checkGlError(gl, "i1 post-glGenVertexArrays error");

//            logger.info("boundaryVertexArrayId = " + boundaryVertexArrayId.get(0));

            gl.glBindVertexArray(boundaryVertexArrayId.get(0));
            checkGlError(gl, "i2 glBindVertexArray error");

            gl.glGenBuffers(1, boundaryVertexBufferId);
            checkGlError(gl, "i3 glGenBuffers() error");

//            logger.info("boundaryVertexBufferId = " + boundaryVertexBufferId.get(0));

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, boundaryVertexBufferId.get(0));
            checkGlError(gl, "i4 glBindBuffer error");

            gl.glBufferData(GL4.GL_ARRAY_BUFFER, boundaryVertexFb.capacity() * 4, boundaryVertexFb, GL4.GL_STATIC_DRAW);
            checkGlError(gl, "i5 glBufferData error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

            //debug
            //gl.glBindVertexArray(0);

//            logger.info("BoundingBoxActor init() finished");
        }

    }

    @Override
    public void display(GL4 gl, GLShaderProgram shader) {

        if (shader instanceof AB2Basic3DShader) {

            AB2Basic3DShader basic3DShader=(AB2Basic3DShader)shader;

//            logger.info("display() start");

            Vector4 actorColor = renderer.getColorIdMap().get(actorId);
            if (actorColor != null) {
                basic3DShader.setColor(gl, actorColor);
            }
            else {
                // debug
                basic3DShader.setColor(gl, new Vector4(0.5f, 0.5f, 0.5f, 1.0f));
            }

            basic3DShader.setMVP(gl, getModelMatrix().multiply(renderer.getVp3d()));

            gl.glBindVertexArray(boundaryVertexArrayId.get(0));
            checkGlError(gl, "d1 glBindVertexArray() error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, boundaryVertexBufferId.get(0));
            checkGlError(gl, "d2 glBindBuffer error");

            gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 24, 0);
            checkGlError(gl, "d5 glVertexAttribPointer()");
            gl.glEnableVertexAttribArray(0);
            checkGlError(gl, "d6 glEnableVertexAttribArray()");
            gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 24, 12);
            checkGlError(gl, "d7 glVertexAttribPointer()");
            gl.glEnableVertexAttribArray(1);
            checkGlError(gl, "d8 glEnableVertexAttribArray()");

            gl.glDrawArrays(GL4.GL_LINES, 0, boundaryVertexFb.capacity() / 6);
            checkGlError(gl, "d7 glDrawArrays() error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
            checkGlError(gl, "d8 glDrawArrays() error");

            gl.glFlush();

//            logger.info("BoundingBoxActor display() finished");
        }

    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {

        if (shader instanceof AB2Basic3DShader) {
            gl.glDeleteVertexArrays(1, boundaryVertexArrayId);
            gl.glDeleteBuffers(1, boundaryVertexBufferId);
        }
    }

}
