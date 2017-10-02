package org.janelia.it.workstation.ab2.gl;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;
import javax.media.opengl.glu.GLU;
import javax.swing.ImageIcon;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.ab2.renderer.AB2SkeletonRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.janelia.it.workstation.ab2.gl.GLAbstractActor.Mode.DRAW;

public abstract class GLAbstractActor {

    protected static GLU glu = new GLU();
    private static Logger logger = LoggerFactory.getLogger(GLAbstractActor.class);

    public enum Mode { DRAW, PICK };

    private static int mvpPrecomputeGroupCount=0;
    public static boolean checkGlErrorActive=false;
    protected Mode mode=DRAW;

    public static synchronized int getNextMvpPrecomputeGroup() {
        mvpPrecomputeGroupCount++;
        return mvpPrecomputeGroupCount;
    }

    int mvpPrecomputeGroup=0;

    protected int pickIndex=-1;

    public void setPickIndex(int pickIndex) { this.pickIndex=pickIndex; }

    public int getPickIndex() { return pickIndex; }

    protected GLActorUpdateCallback actorCallback;

    protected Matrix4 model=new Matrix4();

    //protected boolean isVisible=true;

    protected int actorId=0;

    public Mode getMode() { return mode; }

    public void setMode(Mode mode) { this.mode=mode; }

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