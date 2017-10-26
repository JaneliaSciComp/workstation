package org.janelia.it.workstation.ab2.actor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Quaternion;
import org.janelia.geometry3d.Rotation;
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

        logger.info("init() start");

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

        logger.info("Camera3DFollowBoxActor init() finished");

    }

    @Override
    public void display(GL4 gl, GLShaderProgram shader) {

        logger.info("display() start");

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

        logger.info("Camera3DFollowBoxActor display() finished");

    }

    @Override
    public void dispose(GL4 gl, GLShaderProgram shader) {
        gl.glDeleteVertexArrays(1, boundaryVertexArrayId);
        gl.glDeleteBuffers(1, boundaryVertexBufferId);
    }

    public Matrix4 getModelMatrix() {
        //if (modelMatrix==null) {
            Matrix4 translationMatrix = new Matrix4();
            translationMatrix.set(
                    1.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 1.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 1.0f, 0.0f,
                    -0.5f, -0.5f, -0.5f, 1.0f);
            Matrix4 scaleMatrix = new Matrix4();
            scaleMatrix.set(
                    3.5f, 0.0f, 0.0f, 0.0f,
                    0.0f, 3.5f, 0.0f, 0.0f,
                    0.0f, 0.0f, 3.5f, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f);
            modelMatrix=translationMatrix.multiply(scaleMatrix);
        //}
        Rotation r=new Rotation();
        r.copy(renderer.getRotation());
        r.transpose();
        Vector3 v=r.multiply(new Vector3(renderer.getFocusPosition3d()));
        float d=renderer.getFocusDistance3d();
        float xh=(float)Math.sqrt((double)(v.getX()*v.getX()+d*d));
        float yh=(float)Math.sqrt((double)(v.getY()*v.getY()+d*d));
        //logger.info("v="+v.toString()+" d="+d);
        float yAngle=-1f*(float)Math.asin((double)(v.getX()/xh));
        float xAngle=(float)Math.asin((double)(v.getY()/yh));

        // The axes about which to rotate by these angles depends on the Y and X axes
        // being first rotated into the current reference frame, using
        // Rotation.setFromAxisAngle(axis, angle).
        //Vector3 rX=r.multiply(new Vector3(1f, 0f, 0f));
        //Vector3 rY=r.multiply(new Vector3(0f, 1f, 0f));

        Vector3 rX=new Vector3(1f, 0f, 0f);
        Vector3 rY=new Vector3(0f, 1f, 0f);

        // TODO: debug Quaternion version to match Rotation version

        //Quaternion qR=r.convertRotationToQuaternion();
        //Quaternion qY=new Quaternion().setFromAxisAngle(rX, xAngle);
        //Quaternion qX=new Quaternion().setFromAxisAngle(rY, yAngle);

        Rotation yRotation=new Rotation();
        yRotation.setFromAxisAngle(rX, xAngle);
        Rotation xRotation=new Rotation();
        xRotation.setFromAxisAngle(rY, yAngle);

        //Quaternion xr=multiplyQuaternions(qX, qR);
        //Quaternion yr=multiplyQuaternions(qY, xr);
        //Rotation yR=new Rotation().setFromQuaternion(yr);

      return new Matrix4(modelMatrix).multiply(yRotation.asTransform()).multiply(xRotation.asTransform()).multiply(r.asTransform());

        //return new Matrix4(modelMatrix).multiply(yR.asTransform());
    }

    protected Quaternion multiplyQuaternions(Quaternion qQ, Quaternion rQ) {
        Quaternion tQ=new Quaternion();
        float[] t=tQ.asArray();
        float[] q=qQ.asArray();
        float[] r=rQ.asArray();

        t[0]=r[0]*q[0]-r[1]*q[1]-r[2]*q[2]-r[3]*q[3];
        t[1]=r[0]*q[1]+r[1]*q[0]-r[2]*q[3]+r[3]*q[2];
        t[2]=r[0]*q[2]+r[1]*q[3]+r[2]*q[0]-r[3]*q[1];
        t[3]=r[0]*q[3]-r[1]*q[2]+r[2]*q[1]+r[3]*q[0];

        return tQ;
    }

}

