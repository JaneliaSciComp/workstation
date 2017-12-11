package org.janelia.it.workstation.ab2.actor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer3D;
import org.janelia.it.workstation.ab2.shader.AB2Basic3DShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PointSetActor extends GLAbstractActor {

    private final Logger logger = LoggerFactory.getLogger(PointSetActor.class);

    List<Vector3> points;

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);

    FloatBuffer pointVertexFb;

    public PointSetActor(AB2Renderer3D renderer, int actorId, List<Vector3> points) {
        super(renderer);
        this.actorId=actorId;
        this.points=points;
    }

    @Override
    public void init(GL4 gl, GLShaderProgram shader) {

        logger.info("init() start");

        if (shader instanceof AB2Basic3DShader) {

            AB2Basic3DShader basic3DShader = (AB2Basic3DShader) shader;

            float[] pointData = new float[points.size() * 3];

            for (int i = 0; i < points.size(); i++) {
                Vector3 v = points.get(i);
                pointData[i * 3] = v.getX();
                pointData[i * 3 + 1] = v.getY();
                pointData[i * 3 + 2] = v.getZ();
            }

            pointVertexFb = GLAbstractActor.createGLFloatBuffer(pointData);

            gl.glGenVertexArrays(1, vertexArrayId);
            checkGlError(gl, "i1 glGenVertexArrays error");

            gl.glBindVertexArray(vertexArrayId.get(0));
            checkGlError(gl, "i2 glBindVertexArray error");

            gl.glGenBuffers(1, vertexBufferId);
            checkGlError(gl, "i3 glGenBuffers() error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            checkGlError(gl, "i4 glBindBuffer error");

            gl.glBufferData(GL4.GL_ARRAY_BUFFER, pointVertexFb.capacity() * 4, pointVertexFb, GL4.GL_STATIC_DRAW);
            checkGlError(gl, "i5 glBufferData error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

            logger.info("init() done");

        }

    }

    @Override
    public void display(GL4 gl, GLShaderProgram shader) {

        logger.info("display() start");

        if (shader instanceof AB2Basic3DShader) {
            AB2Basic3DShader basic3DShader = (AB2Basic3DShader) shader;
            basic3DShader.setMVP(gl, getModelMatrix().multiply(renderer.getVp3d()));
            Vector4 actorColor = renderer.getColorIdMap().get(actorId);
            if (actorColor != null) {
                basic3DShader.setColor(gl, actorColor);
            }

            gl.glPointSize(3.0f);

            gl.glBindVertexArray(vertexArrayId.get(0));
            checkGlError(gl, "d1 glBindVertexArray() error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            checkGlError(gl, "d2 glBindBuffer error");

            gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
            checkGlError(gl, "d3 glVertexAttribPointer 0 () error");

            gl.glEnableVertexAttribArray(0);
            checkGlError(gl, "d4 glEnableVertexAttribArray 0 () error");

            gl.glDrawArrays(GL4.GL_POINTS, 0, pointVertexFb.capacity() / 3);
            checkGlError(gl, "d7 glDrawArrays() error");

            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        }

        logger.info("display() end");

    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {
        if (shader instanceof AB2Basic3DShader) {
            gl.glDeleteVertexArrays(1, vertexArrayId);
            gl.glDeleteBuffers(1, vertexBufferId);
        }
    }



}
