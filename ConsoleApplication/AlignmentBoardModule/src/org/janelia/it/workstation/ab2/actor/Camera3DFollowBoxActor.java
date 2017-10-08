package org.janelia.it.workstation.ab2.actor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.renderer.AB23DRenderer;
import org.janelia.it.workstation.ab2.shader.AB2ActorShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Camera3DFollowBoxActor extends GLAbstractActor
{
    private final Logger logger = LoggerFactory.getLogger(Camera3DFollowBoxActor.class);

    Vector3 v0;
    Vector3 v1;

    IntBuffer boundaryVertexArrayId=IntBuffer.allocate(1);
    IntBuffer boundaryVertexBufferId=IntBuffer.allocate(1);

    FloatBuffer boundaryVertexFb;

    public Camera3DFollowBoxActor(AB23DRenderer renderer, int actorId, Vector3 v0, Vector3 v1) {
        super(renderer);
        this.actorId=actorId;
        this.v0=v0;
        this.v1=v1;
    }

    @Override
    public void init(GL4 gl, GLShaderProgram shader) {

        float[] boundaryData = new float[] {
                v0.getX(), v0.getY(), v0.getZ(),
                v0.getX(), v1.getY(), v0.getZ(),

                v0.getX(), v1.getY(), v0.getZ(),
                v1.getX(), v1.getY(), v0.getZ(),

                v1.getX(), v1.getY(), v0.getZ(),
                v1.getX(), v0.getY(), v0.getZ(),

                v1.getX(), v0.getY(), v0.getZ(),
                v0.getX(), v0.getY(), v0.getZ(),

                v0.getX(), v0.getY(), v1.getZ(),
                v0.getX(), v1.getY(), v1.getZ(),

                v0.getX(), v1.getY(), v1.getZ(),
                v1.getX(), v1.getY(), v1.getZ(),

                v1.getX(), v1.getY(), v1.getZ(),
                v1.getX(), v0.getY(), v1.getZ(),

                v1.getX(), v0.getY(), v1.getZ(),
                v0.getX(), v0.getY(), v1.getZ(),

                v0.getX(), v1.getY(), v0.getZ(),
                v0.getX(), v1.getY(), v1.getZ(),

                v0.getX(), v0.getY(), v0.getZ(),
                v0.getX(), v0.getY(), v1.getZ(),

                v1.getX(), v1.getY(), v0.getZ(),
                v1.getX(), v1.getY(), v1.getZ(),

                v1.getX(), v0.getY(), v0.getZ(),
                v1.getX(), v0.getY(), v1.getZ()
        };

        boundaryVertexFb=createGLFloatBuffer(boundaryData);

        gl.glGenVertexArrays(1, boundaryVertexArrayId);
        checkGlError(gl, "i1 glGenVertexArrays error");

        gl.glBindVertexArray(boundaryVertexArrayId.get(0));
        checkGlError(gl, "i2 glBindVertexArray error");

        gl.glGenBuffers(1, boundaryVertexBufferId);
        checkGlError(gl, "i3 glGenBuffers() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, boundaryVertexBufferId.get(0));
        checkGlError(gl, "i4 glBindBuffer error");

        gl.glBufferData(GL4.GL_ARRAY_BUFFER, boundaryVertexFb.capacity() * 4, boundaryVertexFb, GL4.GL_STATIC_DRAW);
        checkGlError(gl, "i5 glBufferData error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        //logger.info("BoundingBoxActor init() finished");

    }

    @Override
    public void display(GL4 gl, GLShaderProgram shader) {

        AB2ActorShader actorShader=(AB2ActorShader)shader;
        actorShader.setMVP3d(gl, getModelMatrix().multiply(renderer.getVp3d()));
        actorShader.setMVP2d(gl, getModelMatrix().multiply(renderer.getVp2d()));
        actorShader.setTwoDimensional(gl, false);
        actorShader.setTextureType(gl, AB2ActorShader.TEXTURE_TYPE_NONE);

        Vector4 actorColor=renderer.getColorIdMap().get(actorId);
        if (actorColor!=null) {
            actorShader.setColor0(gl, actorColor);
        }

        gl.glBindVertexArray(boundaryVertexArrayId.get(0));
        checkGlError(gl, "d1 glBindVertexArray() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, boundaryVertexBufferId.get(0));
        checkGlError(gl, "d2 glBindBuffer error");

        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        checkGlError(gl, "d3 glVertexAttribPointer 0 () error");

        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d4 glEnableVertexAttribArray 0 () error");

        gl.glDrawArrays(GL4.GL_LINES, 0, boundaryVertexFb.capacity()/3);
        checkGlError(gl, "d7 glDrawArrays() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        //logger.info("BoundingBoxActor display() finished");

    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {
        gl.glDeleteVertexArrays(1, boundaryVertexArrayId);
        gl.glDeleteBuffers(1, boundaryVertexBufferId);
    }

}

