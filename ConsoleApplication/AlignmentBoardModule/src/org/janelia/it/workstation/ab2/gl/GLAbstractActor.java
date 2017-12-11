package org.janelia.it.workstation.ab2.gl;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GL4;
import javax.media.opengl.glu.GLU;
import javax.swing.ImageIcon;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.ab2.renderer.AB23DRenderer;
import org.janelia.it.workstation.ab2.renderer.AB2SkeletonRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class GLAbstractActor {

    protected static GLU glu = new GLU();
    private static Logger logger = LoggerFactory.getLogger(GLAbstractActor.class);
    protected AB23DRenderer renderer;

    protected GLAbstractActor(AB23DRenderer renderer) {
        this.renderer=renderer;
    }

    public static boolean checkGlErrorActive=true;

    protected int pickIndex=-1;

    public void setPickIndex(int pickIndex) { this.pickIndex=pickIndex; }

    public int getPickIndex() { return pickIndex; }

    protected Matrix4 modelMatrix;
    protected Matrix4 postProjectionMatrix;

    protected int actorId=0;

    public void setup() {}

    public abstract void dispose(GL4 gl, GLShaderProgram shader);

    public abstract void init(GL4 gl, GLShaderProgram shader);

    public abstract void display(GL4 gl, GLShaderProgram shader);

    public void setId(int id) {
        actorId=id;
    }

    public int getActorId() {
        return actorId;
    }

    public Matrix4 getModelMatrix() {
        if (modelMatrix==null) {
            Matrix4 translationMatrix = new Matrix4();
            translationMatrix.set(
                    1.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 1.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 1.0f, 0.0f,
                    -0.5f, -0.5f, -0.5f, 1.0f);
            Matrix4 scaleMatrix = new Matrix4();
            scaleMatrix.set(
                    2.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 2.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 2.0f, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f);
            modelMatrix=translationMatrix.multiply(scaleMatrix);
        }
        return new Matrix4(modelMatrix);
    }

    public Matrix4 getPostProjectionMatrix() {
        if (postProjectionMatrix!=null) {
            return new Matrix4(postProjectionMatrix);
        } else {
            return null;
        }
    }

    public void setPostProjectionMatrix(Matrix4 postProjectionMatrix) {
        this.postProjectionMatrix=postProjectionMatrix;
    }

    public void setModelMatrix(Matrix4 modelMatrix) { this.modelMatrix=modelMatrix; }

    public static void checkGlError(GL4 gl, String message) {
        if (checkGlErrorActive) {
            int errorNumber = gl.glGetError();
            if (errorNumber <= 0)
                return;
            String errorStr = glu.gluErrorString(errorNumber);
            //String className=this.getClass().getName();
            logger.error("OpenGL error number=" + errorNumber + ": " + errorStr + ": " + message);
        }
    }

    public static IntBuffer createGLIntBuffer(int capacity) {
        int intBytes=Integer.SIZE/Byte.SIZE;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity*intBytes);
        return byteBuffer.order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    public static IntBuffer createGLIntBuffer(int[] intArray) {
        IntBuffer intBuffer = createGLIntBuffer(intArray.length);
        for (int i=0;i<intArray.length;i++) {
            intBuffer.put(i, intArray[i]);
        }
        return intBuffer;
    }

    public static ShortBuffer createGLShortBuffer(int capacity) {
        int shortBytes=Short.SIZE/Byte.SIZE;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity*shortBytes);
        return byteBuffer.order(ByteOrder.nativeOrder()).asShortBuffer();
    }

    public static ShortBuffer createGLShortBuffer(short[] shortArray) {
        ShortBuffer shortBuffer = createGLShortBuffer(shortArray.length);
        for (int i=0;i<shortArray.length;i++) {
            shortBuffer.put(i, shortArray[i]);
        }
        return shortBuffer;
    }

    public static ByteBuffer createGLByteBuffer(int capacity) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity);
        return byteBuffer.order(ByteOrder.nativeOrder());
    }

    public static ByteBuffer createGLByteBuffer(byte[] byteArray) {
        ByteBuffer byteBuffer = createGLByteBuffer(byteArray.length);
        for (int i=0;i<byteArray.length;i++) {
            byteBuffer.put(i, byteArray[i]);
        }
        return byteBuffer.order(ByteOrder.nativeOrder());
    }

    public static FloatBuffer createGLFloatBuffer(int capacity) {
        int floatBytes=Float.SIZE/Byte.SIZE;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity*floatBytes);
        return byteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public static FloatBuffer createGLFloatBuffer(float[] floatArray) {
        FloatBuffer floatBuffer=createGLFloatBuffer(floatArray.length);
        for (int i=0;i<floatArray.length;i++) {
            floatBuffer.put(i, floatArray[i]);
        }
        return floatBuffer;
    }

    public boolean isTwoDimensional() {
        return false;
    }

    public static BufferedImage getImageByFilename(String filename) {
        URL picURL = AB2SkeletonRenderer.class.getResource("/org/janelia/it/workstation/ab2/images/" + filename);
        if (picURL==null) {
            logger.error("Could not find image="+filename);
            return null;
        }
        ImageIcon imageIcon = new ImageIcon(picURL);
        Image source = imageIcon.getImage();
        int w = source.getWidth(null);
        int h = source.getHeight(null);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D)image.getGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return image;
    }


}