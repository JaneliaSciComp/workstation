package org.janelia.it.workstation.ab2.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;
import javax.media.opengl.glu.GLU;

import org.janelia.geometry3d.Matrix4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GLAbstractActor {

    protected static GLU glu = new GLU();
    private static Logger logger = LoggerFactory.getLogger(GLAbstractActor.class);

    private static int mvpPrecomputeGroupCount=0;

    public static boolean checkGlErrorActive=false;

    public static synchronized int getNextMvpPrecomputeGroup() {
        mvpPrecomputeGroupCount++;
        return mvpPrecomputeGroupCount;
    }

    int mvpPrecomputeGroup=0;

    protected GLActorUpdateCallback actorCallback;

    protected Matrix4 model=new Matrix4();

    //protected boolean isVisible=true;

    protected int actorId=0;

    // Callable within a non-GL setup thread before init()
    public void setup() {}

    public abstract void dispose(GL4 gl);

    public abstract void init(GL4 gl);

    public void display(GL4 gl) {
        if (actorCallback!=null) {
            actorCallback.update(gl, this);
        }
    }

    public void setId(int id) {
        actorId=id;
    }

    public int getActorId() {
        return actorId;
    }

    public Matrix4 getModel() { return model; }

    public void setModel(Matrix4 model) { this.model=model; }

    public void setUpdateCallback(GLActorUpdateCallback updateCallback) {
        this.actorCallback=updateCallback;
    }

    protected void checkGlError(GL4 gl, String message) {
        if (checkGlErrorActive) {
            int errorNumber = gl.glGetError();
            if (errorNumber <= 0)
                return;
            String errorStr = glu.gluErrorString(errorNumber);
            String className=this.getClass().getName();
            logger.error("OpenGL error in "+className+" number=" + errorNumber + ": " + errorStr + ": " + message);
        }
    }

//    public boolean isVisible() {
//        return isVisible;
//    }
//
//    public void setIsVisible(boolean isVisible) {
//        this.isVisible = isVisible;
//    }

    public int getMvpPrecomputeGroup() {
        return mvpPrecomputeGroup;
    }

    public void setMvpPrecomputeGroup(int mvpPrecomputeGroup) {
        this.mvpPrecomputeGroup = mvpPrecomputeGroup;
    }

    public static IntBuffer createGLIntBuffer(int capacity) {
        int intBytes=Integer.SIZE/Byte.SIZE;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity*intBytes);
        return byteBuffer.order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    public static FloatBuffer createGLFloatBuffer(int capacity) {
        int floatBytes=Float.SIZE/Byte.SIZE;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity*floatBytes);
        return byteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public static IntBuffer createGLIntBuffer(int[] intArray) {
        IntBuffer intBuffer=createGLIntBuffer(intArray.length);
        for (int i=0;i<intArray.length;i++) {
            intBuffer.put(i, intArray[i]);
        }
        return intBuffer;
    }

    public static FloatBuffer createGLFloatBuffer(float[] floatArray) {
        FloatBuffer floatBuffer=createGLFloatBuffer(floatArray.length);
        for (int i=0;i<floatArray.length;i++) {
            floatBuffer.put(i, floatArray[i]);
        }
        return floatBuffer;
    }
}