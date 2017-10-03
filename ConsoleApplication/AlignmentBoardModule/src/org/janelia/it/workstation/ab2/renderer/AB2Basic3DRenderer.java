package org.janelia.it.workstation.ab2.renderer;

import java.awt.Point;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.geometry3d.Viewport;

import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AB2Basic3DRenderer extends AB23DRenderer {

    private final Logger logger = LoggerFactory.getLogger(AB2Basic3DRenderer.class);

    FloatBuffer backgroundColorBuffer=FloatBuffer.allocate(4);
    Vector4 backgroundColor=new Vector4(0.0f, 0.0f, 0.0f, 0.0f);


    private void setBackgroundColorBuffer() {
        backgroundColorBuffer.put(0,backgroundColor.get(0));
        backgroundColorBuffer.put(1,backgroundColor.get(1));
        backgroundColorBuffer.put(2,backgroundColor.get(2));
        backgroundColorBuffer.put(3,backgroundColor.get(3));
    }

    public AB2Basic3DRenderer(GLShaderProgram drawShader, GLShaderProgram pickShader) {
        super(drawShader, pickShader);
        setBackgroundColorBuffer();
    }

    protected Matrix4 getModelMatrix() {
        Matrix4 modelMatrix=new Matrix4(); // default constructor is identity
        return modelMatrix;
    }

    public void display(GL4 gl) {
        displaySync(gl);
    }

    protected synchronized void displaySync(GL4 gl) {

        gl.glClear(GL4.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        gl.glClearBufferfv(gl.GL_COLOR, 0, backgroundColorBuffer);

        Matrix4 modelMatrix3d=new Matrix4(getModelMatrix());
        Matrix4 modelMatrix2d=new Matrix4(getModelMatrix());

        Matrix4 projectionMatrix3d=new Matrix4(camera3d.getProjectionMatrix());
        Matrix4 viewMatrix3d=new Matrix4(camera3d.getViewMatrix());
        mvp3d=modelMatrix3d.multiply(viewMatrix3d.multiply(projectionMatrix3d));

        Matrix4 projectionMatrix2d=new Matrix4(camera2d.getProjectionMatrix());
        Matrix4 viewMatrix2d=new Matrix4(camera2d.getViewMatrix());
        mvp2d=modelMatrix2d.multiply(viewMatrix2d.multiply(projectionMatrix2d));

        //logger.info("Check1.0");
        drawActionSequence.display(gl);
        //logger.info("Check1.1");

        if (mouseClickEvents.size()>0 && pickActionSequence.getActorSequence().size()>0) {

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
              //drawBuffer.put(GL4.GL_COLOR_ATTACHMENT0);
              gl.glDrawBuffers(1, drawBuffer);

              //logger.info("pickActionSequence.display() start");

              pickActionSequence.display(gl);

              //logger.info("pickActionSequence.display() end");

//
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

        vantage.getRotationInGround().multiply(rotation.transpose());
    }

    public void translatePixels(double dx, double dy, double dz) {
        // trackball translate
        Vector3 translation=new Vector3((float)-dx, (float)dy, (float)-dz);
        translation=translation.multiplyScalar((float)glUnitsPerPixel());
        Rotation copyOfRotation = new Rotation(vantage.getRotationInGround());
        translation=copyOfRotation.multiply(translation);
        vantage.getFocusPosition().add(translation);
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

        vantage.setSceneUnitsPerViewportHeight((float)sceneUnitsPerViewportHeight);
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

}
