package org.janelia.it.workstation.ab2.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.ab2.gl.GLDisplayUpdateCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AB23DRenderer implements AB2Renderer3DControls {
    Logger logger = LoggerFactory.getLogger(AB23DRenderer.class);
    protected static GLU glu = new GLU();


    protected void checkGlError(GL4 gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );
    }

    public abstract void init(GL4 gl);

    public abstract void dispose(GL4 gl);

    public abstract void display(GL4 gl);

    public abstract void reshape(GL4 gl, int x, int y, int width, int height);

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
