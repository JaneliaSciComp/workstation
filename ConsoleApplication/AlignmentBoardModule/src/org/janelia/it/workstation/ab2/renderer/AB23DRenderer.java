package org.janelia.it.workstation.ab2.renderer;

import java.awt.Point;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.media.opengl.GL4;
import javax.media.opengl.glu.GLU;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.OrthographicCamera;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.geometry3d.Viewport;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AB23DRenderer implements AB2Renderer3DControls {
    Logger logger = LoggerFactory.getLogger(AB23DRenderer.class);
    protected static GLU glu = new GLU();

    public static final double DISTANCE_TO_SCREEN_IN_PIXELS = 2500;
    protected static final double MAX_CAMERA_FOCUS_DISTANCE = 1000000.0;
    protected static final double MIN_CAMERA_FOCUS_DISTANCE = 0.001;
    public static final double DEFAULT_CAMERA_FOCUS_DISTANCE = 0.0;

    FloatBuffer backgroundColorBuffer=FloatBuffer.allocate(4);
    Vector4 backgroundColor=new Vector4(0.0f, 0.0f, 0.0f, 0.0f);

    protected Viewport viewport;
    protected PerspectiveCamera camera3d;
    protected OrthographicCamera camera2d;
    protected Vantage vantage3d;
    protected Vantage vantage2d;

    Matrix4 vp3d;
    Matrix4 vp2d;

    protected IntBuffer pickFramebufferId;
    protected IntBuffer pickColorTextureId;
    protected IntBuffer pickDepthTextureId;

    protected List<GLShaderActionSequence> drawShaderList=new ArrayList<>();
    protected List<GLShaderActionSequence> pickShaderList=new ArrayList<>();

    public void addDrawShaderActionSequence(GLShaderActionSequence shaderActionSequence) {
        drawShaderList.add(shaderActionSequence);
    }

    public void addPickShaderActionSequence(GLShaderActionSequence shaderActionSequence) {
        pickShaderList.add(shaderActionSequence);
    }

    public GLShaderActionSequence getDrawShaderActionSequenceByIndex(int i) { return drawShaderList.get(i); }

    public GLShaderActionSequence getPickShaderActionSequenceByIndex(int i) { return pickShaderList.get(i); }

    Map<Integer, Vector4> colorIdMap=new HashMap<>();

    public Map<Integer, Vector4> getColorIdMap() { return colorIdMap; }

    protected boolean initialized=false;

    protected ConcurrentLinkedDeque<MouseClickEvent> mouseClickEvents=new ConcurrentLinkedDeque<>();

    protected class MouseClickEvent {
        public int x=0;
        public int y=0;
        public MouseClickEvent(int x, int y) { this.x=x; this.y=y; }
    }

    public void addMouseClickEvent(int x, int y) {
        mouseClickEvents.add(new MouseClickEvent(x,y));
    }

    protected void setBackgroundColorBuffer() {
        backgroundColorBuffer.put(0,backgroundColor.get(0));
        backgroundColorBuffer.put(1,backgroundColor.get(1));
        backgroundColorBuffer.put(2,backgroundColor.get(2));
        backgroundColorBuffer.put(3,backgroundColor.get(3));
    }

    public AB23DRenderer() {
        setBackgroundColorBuffer();
        vantage3d=new Vantage(null);
        vantage2d=new Vantage(null);
        viewport=new Viewport();
        viewport.setzNearRelative(0.1f);
        camera3d = new PerspectiveCamera(vantage3d, viewport);
        camera2d = new OrthographicCamera(vantage2d, viewport);
        vantage3d.setFocus(0.0f,0.0f,(float)DEFAULT_CAMERA_FOCUS_DISTANCE);
        vantage2d.setFocus(0.0f,0.0f,(float)DEFAULT_CAMERA_FOCUS_DISTANCE);
    }

    protected void checkGlError(GL4 gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );
    }

    public void init(GL4 gl) {
        initSync(gl);
    }

    protected synchronized void initSync(GL4 gl) {
        try {

            for (GLShaderActionSequence shaderActionSequence : drawShaderList) {
                shaderActionSequence.init(gl);
            }

            for (GLShaderActionSequence shaderActionSequence : pickShaderList) {
                shaderActionSequence.init(gl);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void dispose(GL4 gl) {

        for (GLShaderActionSequence shaderActionSequence : drawShaderList) {
            shaderActionSequence.dispose(gl);
        }

        for (GLShaderActionSequence shaderActionSequence : pickShaderList) {
            shaderActionSequence.dispose(gl);
        }
    }

    public Matrix4 getVp3d() { return new Matrix4(vp3d); }

    public Matrix4 getVp2d() { return new Matrix4(vp2d); }

    public void display(GL4 gl) {
        displaySync(gl);
    }

    protected synchronized void displaySync(GL4 gl) {

        gl.glClear(GL4.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        gl.glClearBufferfv(gl.GL_COLOR, 0, backgroundColorBuffer);

        Matrix4 projectionMatrix3d=new Matrix4(camera3d.getProjectionMatrix());
        Matrix4 viewMatrix3d=new Matrix4(camera3d.getViewMatrix());
        vp3d=viewMatrix3d.multiply(projectionMatrix3d);
//        mvp3d=modelMatrix3d.multiply(viewMatrix3d.multiply(projectionMatrix3d));

        Matrix4 projectionMatrix2d=new Matrix4(camera2d.getProjectionMatrix());
        Matrix4 viewMatrix2d=new Matrix4(camera2d.getViewMatrix());
        vp2d=viewMatrix2d.multiply(projectionMatrix2d);
//        mvp2d=modelMatrix2d.multiply(viewMatrix2d.multiply(projectionMatrix2d));

        for (GLShaderActionSequence shaderActionSequence : drawShaderList) {
            shaderActionSequence.display(gl);
        }

        if (mouseClickEvents.size()>0 && pickShaderList.size()>0) {

            // From: https://www.opengl.org/discussion_boards/showthread.php/198703-Framebuffer-Integer-Texture-Attachment
            //
            // NOTE: this needs to be tested on different OSes
            //
            // To encode integer values:
            //
            //    glTexImage2D(GL_TEXTURE_2D, 0, GL_R32I, width, height, 0, GL_RED_INTEGER, GL_INT, 0);
            //
            // To read:
            //
            //    int pixel = 0;
            //    glReadBuffer(GL_COLOR_ATTACHMENT1); // whereever the texture is attached to ...
            //    glReadPixels(x, y, 1, 1, GL_RED_INTEGER, GL_INT, &pixel);
            //
            //

            gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, pickFramebufferId.get(0));
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClearDepth(1.0f);
            gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

            // NOTE: it is necessary to use array here rather than just insert
            // to buffer, in order to get native byte order correct
            int[] drawBuffersTargets = new int[]{
                    GL4.GL_COLOR_ATTACHMENT0
            };
            IntBuffer drawBuffer = IntBuffer.allocate(1);
            drawBuffer.put(0, drawBuffersTargets[0]);
            gl.glDrawBuffers(1, drawBuffer);

            for (GLShaderActionSequence shaderActionSequence : pickShaderList) {
                shaderActionSequence.display(gl);
            }

            while (mouseClickEvents.size()>0) {
                //logger.info("displaySync() processing mouse click");
                MouseClickEvent mouseClickEvent=mouseClickEvents.poll();
                if (mouseClickEvent!=null) {
                    //logger.info("Pick at x="+mouseClickEvent.x+" y="+mouseClickEvent.y);
                    int pickId=getPickIdAtXY(gl,mouseClickEvent.x, mouseClickEvent.y, true, false);
                    //logger.info("Pick id at x="+mouseClickEvent.x+" y="+mouseClickEvent.y+" is="+pickId);
                    // Lookup event
                    if (pickId>0) {
                        AB2Event pickEvent = AB2Controller.getController().getPickEvent(pickId);
                        if (pickEvent!=null) {
                            AB2Controller.getController().addEvent(pickEvent);
                        }
                    }
                }
            }

            gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, 0);
        }

    }

    public double glUnitsPerPixel() {
        return Math.abs( camera3d.getCameraFocusDistance() ) / DISTANCE_TO_SCREEN_IN_PIXELS;
    }

    public void reshape(GL4 gl, int x, int y, int width, int height) {
        //logger.info("reshape() x="+x+" y="+y+" width="+width+" height="+height);
        viewport.setHeightPixels(height);
        viewport.setWidthPixels(width);
        viewport.getChangeObservable().notifyObservers();
        resetPickFramebuffer(gl, width, height);
        if (initialized) display(gl);
    }


    public void rotatePixels(double dx, double dy, double dz) {

        // Rotate like a trackball
        double dragDistance = Math.sqrt(dy * dy + dx * dx + dz * dz);
        if (dragDistance <= 0.0)
            return;

        Vector3 rotationAxis=new Vector3( (float)dy, (float)dx, (float)dz);
        rotationAxis=rotationAxis.normalize();

        double wD=viewport.getWidthPixels() * 1.0;
        double hD=viewport.getHeightPixels() * 1.0;

        double windowSize = Math.sqrt(wD*wD + hD*hD);

        // Drag across the entire window to rotate all the way around
        double rotationAngle = 2.0 * Math.PI * dragDistance/windowSize;

        Rotation rotation = new Rotation().setFromAxisAngle(rotationAxis, (float)rotationAngle);

        vantage3d.getRotationInGround().multiply(rotation.transpose());
    }

    public void translatePixels(double dx, double dy, double dz) {
        // trackball translate
        Vector3 translation=new Vector3((float)-dx, (float)dy, (float)-dz);
        translation=translation.multiplyScalar((float)glUnitsPerPixel());
        Rotation copyOfRotation = new Rotation(vantage3d.getRotationInGround());
        translation=copyOfRotation.multiply(translation);
        vantage3d.getFocusPosition().add(translation);
    }

    public void zoom(double zoomRatio) {
        if (zoomRatio <= 0.0) {
            return;
        }
        if (zoomRatio == 1.0) {
            return;
        }

        double cameraFocusDistance = (double)camera3d.getCameraFocusDistance();
        cameraFocusDistance /= zoomRatio;
        if ( cameraFocusDistance > MAX_CAMERA_FOCUS_DISTANCE ) {
            return;
        }
        if ( cameraFocusDistance < MIN_CAMERA_FOCUS_DISTANCE ) {
            return;
        }

// From PerspectiveCamera():
//        public float getCameraFocusDistance() {
//            return 0.5f * vantage.getSceneUnitsPerViewportHeight()
//                    / (float) Math.tan( 0.5 * fovYRadians );
//        }

        double sceneUnitsPerViewportHeight=cameraFocusDistance*Math.tan(0.5*camera3d.getFovRadians())*2.0;

        vantage3d.setSceneUnitsPerViewportHeight((float)sceneUnitsPerViewportHeight);
    }

    public void zoomPixels(Point newPoint, Point oldPoint) {
        // Are we dragging away from the center, or toward the center?
        double wD=viewport.getWidthPixels()*1.0;
        double hD=viewport.getHeightPixels()*1.0;
        double[] p0 = {oldPoint.x - wD/2.0,
                oldPoint.y - hD/2.0};
        double[] p1 = {newPoint.x - wD/2.0,
                newPoint.y - hD/2.0};
        double dC0 = Math.sqrt(p0[0] * p0[0] + p0[1] * p0[1]);
        double dC1 = Math.sqrt(p1[0]*p1[0]+p1[1]*p1[1]);
        double dC = dC1 - dC0; // positive means away
        double denom = Math.max(20.0, dC1);
        double zoomRatio = 1.0 + dC/denom;

        //double cameraFocusDistance=(double)camera.getCameraFocusDistance();

        //logger.info("cfd="+cameraFocusDistance+" setting zoom to="+zoomRatio);

        zoom(zoomRatio);
    }

    protected int getPickIdAtXY(GL4 gl, int x, int y, boolean invertY, boolean bindFramebuffer) {
        //logger.info("getPickIdAtXY x="+x+" y="+y+" invert="+invertY);
        int fX=x;
        int fY=y;
        if (invertY) {
            fY=viewport.getHeightPixels()-y-1;
            if (fY<0) { fY=0; }
        }
        if (bindFramebuffer) { gl.glBindFramebuffer(GL4.GL_READ_FRAMEBUFFER, pickFramebufferId.get(0)); }
        byte[] pixels = readPixels(gl, pickColorTextureId.get(0), GL4.GL_COLOR_ATTACHMENT0, fX, fY, 1, 1);
        int id = getId(pixels);

        /*
        byte[] pixels = readPixels(gl, pickColorTextureId.get(0), GL4.GL_COLOR_ATTACHMENT0, 0, 0,
                viewport.getWidthPixels(), viewport.getHeightPixels());

        logger.info("readPixels() returned "+pixels.length+" pixels");

        int positiveCount=0;
        int negativeCount=0;
        for (int i=0;i<pixels.length;i++) {
            if (pixels[i]>0) {
                positiveCount++;
            } else if (pixels[i]<0) {
                negativeCount++;
            }
        }
        logger.info("positiveCount="+positiveCount+" negativeCount="+negativeCount);
        */

        if (bindFramebuffer) { gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, 0); }
        return id; // debug
    }

    private byte[] readPixels(GL4 gl, int textureId, int attachment, int startX, int startY, int width, int height) {
        gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId);
        int pixelSize = Integer.SIZE/Byte.SIZE;
        int bufferSize = width * height * pixelSize;
        byte[] rawBuffer = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.wrap(rawBuffer);
        gl.glReadBuffer(attachment);
        //logger.info("glReadPixels() startX="+startX+" startY="+startY+" width="+width+" height="+height);
        gl.glReadPixels(startX, startY, width, height, GL4.GL_RED_INTEGER, GL4.GL_INT, buffer);
        checkGlError(gl, "AB23DRenderer readPixels(), after glReadPixels()");
        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
        return rawBuffer;
    }

    private int getId(byte[] rawBuffer) {
//        for (int i=0;i<rawBuffer.length;i++) {
//            logger.info("getId byte "+i+" = "+rawBuffer[i]);
//        }
        ByteBuffer bb = ByteBuffer.wrap(rawBuffer);

        if(ByteOrder.nativeOrder()==ByteOrder.LITTLE_ENDIAN)
            bb.order(ByteOrder.LITTLE_ENDIAN);

        return bb.getInt();
    }

    protected void disposePickFramebuffer(GL4 gl) {
        if (pickDepthTextureId!=null) {
            gl.glDeleteTextures(1, pickDepthTextureId);
            pickDepthTextureId=null;
        }
        if (pickColorTextureId!=null) {
            gl.glDeleteTextures(1, pickColorTextureId);
            pickColorTextureId=null;
        }
        if (pickFramebufferId!=null) {
            gl.glDeleteFramebuffers(1, pickFramebufferId);
            pickFramebufferId=null;
        }
    }

    protected void resetPickFramebuffer(GL4 gl, int width, int height) {
        disposePickFramebuffer(gl);

        pickFramebufferId=IntBuffer.allocate(1);
        gl.glGenFramebuffers(1, pickFramebufferId);
        gl.glBindFramebuffer(gl.GL_FRAMEBUFFER, pickFramebufferId.get(0));

        pickColorTextureId=IntBuffer.allocate(1);
        gl.glGenTextures(1, pickColorTextureId);
        gl.glBindTexture(gl.GL_TEXTURE_2D, pickColorTextureId.get(0));
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_R32I, width, height, 0, GL4.GL_RED_INTEGER, GL4.GL_INT, null);

        pickDepthTextureId=IntBuffer.allocate(1);
        gl.glGenTextures(1, pickDepthTextureId);
        gl.glBindTexture(gl.GL_TEXTURE_2D, pickDepthTextureId.get(0));
        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_DEPTH_COMPONENT, width, height, 0, GL4.GL_DEPTH_COMPONENT, GL4.GL_FLOAT, null);

        gl.glFramebufferTexture(gl.GL_FRAMEBUFFER, gl.GL_COLOR_ATTACHMENT0, pickColorTextureId.get(0), 0);
        gl.glFramebufferTexture(gl.GL_FRAMEBUFFER, gl.GL_DEPTH_ATTACHMENT, pickDepthTextureId.get(0), 0);

        int status = gl.glCheckFramebufferStatus(GL4.GL_FRAMEBUFFER);
        if (status != GL4.GL_FRAMEBUFFER_COMPLETE) {
            logger.error("Failed to establish framebuffer: {}", decodeFramebufferStatus(status));
        }
        else {
            // logger.info("Picking Framebuffer complete.");
        }

        gl.glBindFramebuffer(gl.GL_FRAMEBUFFER, 0);
        gl.glBindTexture(gl.GL_TEXTURE_2D, 0);

    }

    private String decodeFramebufferStatus( int status ) {
        String rtnVal = null;
        switch (status) {
            case GL4.GL_FRAMEBUFFER_UNDEFINED:
                rtnVal = "GL_FRAMEBUFFER_UNDEFINED means target is the default framebuffer, but the default framebuffer does not exist.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT :
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT is returned if any of the framebuffer attachment points are framebuffer incomplete.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT is returned if the framebuffer does not have at least one image attached to it.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER is returned if the value of GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE\n" +
                        "		 is GL_NONE for any color attachment point(s) named by GL_DRAW_BUFFERi.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER is returned if GL_READ_BUFFER is not GL_NONE\n" +
                        "		 and the value of GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE is GL_NONE for the color attachment point named\n" +
                        "		 by GL_READ_BUFFER.";
                break;
            case GL4.GL_FRAMEBUFFER_UNSUPPORTED:
                rtnVal = "GL_FRAMEBUFFER_UNSUPPORTED is returned if the combination of internal formats of the attached images violates\n" +
                        "		 an implementation-dependent set of restrictions.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE is returned if the value of GL_RENDERBUFFER_SAMPLES is not the same\n" +
                        "		 for all attached renderbuffers; if the value of GL_TEXTURE_SAMPLES is the not same for all attached textures; or, if the attached\n" +
                        "		 images are a mix of renderbuffers and textures, the value of GL_RENDERBUFFER_SAMPLES does not match the value of\n" +
                        "		 GL_TEXTURE_SAMPLES.  GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE  also returned if the value of GL_TEXTURE_FIXED_SAMPLE_LOCATIONS is\n" +
                        "		 not the same for all attached textures; or, if the attached images are a mix of renderbuffers and textures, the value of GL_TEXTURE_FIXED_SAMPLE_LOCATIONS\n" +
                        "		 is not GL_TRUE for all attached textures.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS:
                rtnVal = " is returned if any framebuffer attachment is layered, and any populated attachment is not layered,\n" +
                        "		 or if all populated color attachments are not from textures of the same target.";
                break;
            default:
                rtnVal = "--Message not decoded: " + status;
        }
        return rtnVal;
    }

}
