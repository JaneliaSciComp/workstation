package org.janelia.it.workstation.ab2.renderer;

import java.awt.Point;
import java.nio.FloatBuffer;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.geometry3d.Viewport;

import org.janelia.it.workstation.ab2.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AB2Basic3DRenderer extends AB23DRenderer {

    private final Logger logger = LoggerFactory.getLogger(AB2Basic3DRenderer.class);

    protected PerspectiveCamera camera;
    protected Vantage vantage;
    protected Viewport viewport;

    public static final double DEFAULT_CAMERA_FOCUS_DISTANCE = 2.0;

    private static long gl_display_count=0L;

    FloatBuffer backgroundColorBuffer=FloatBuffer.allocate(4);

    Vector4 backgroundColor=new Vector4(0.0f, 0.0f, 0.0f, 0.0f);

    public static final double DISTANCE_TO_SCREEN_IN_PIXELS = 2500;
    private static final double MAX_CAMERA_FOCUS_DISTANCE = 1000000.0;
    private static final double MIN_CAMERA_FOCUS_DISTANCE = 0.001;

    Matrix4 mvp;

    GLShaderActionSequence shaderActionSequence=new GLShaderActionSequence(AB2Basic3DRenderer.class.getName());
    final GLShaderProgram shader;

    private void setBackgroundColorBuffer() {
        backgroundColorBuffer.put(0,backgroundColor.get(0));
        backgroundColorBuffer.put(1,backgroundColor.get(1));
        backgroundColorBuffer.put(2,backgroundColor.get(2));
        backgroundColorBuffer.put(3,backgroundColor.get(3));
    }

    public AB2Basic3DRenderer(GLShaderProgram shader) {
        setBackgroundColorBuffer();
        this.shader=shader;
        vantage=new Vantage(null);
        viewport=new Viewport();
        camera = new PerspectiveCamera(vantage, viewport);
        vantage.setFocus(0.0f,0.0f,(float)DEFAULT_CAMERA_FOCUS_DISTANCE);
    }

    public void init(GL4 gl) {
        initSync(gl);
    }

    protected synchronized void initSync(GL4 gl) {
        try {
            shader.setUpdateCallback(getDisplayUpdateCallback());
            shaderActionSequence.setShader(shader);
            shaderActionSequence.setApplyMemoryBarrier(false);
            shaderActionSequence.init(gl);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected abstract GLDisplayUpdateCallback getDisplayUpdateCallback();

    public void dispose(GL4 gl) {
        shaderActionSequence.dispose(gl);
    }

    public void display(GL4 gl) {
        displaySync(gl);
    }

    protected Matrix4 getModelMatrix() {
        Matrix4 modelMatrix=new Matrix4(); // default constructor is identity
        return modelMatrix;
    }

    protected synchronized void displaySync(GL4 gl) {
        gl.glClear(GL4.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glClearBufferfv(gl.GL_COLOR, 0, backgroundColorBuffer);

        Matrix4 projectionMatrix=new Matrix4(camera.getProjectionMatrix());
        Matrix4 viewMatrix=new Matrix4(camera.getViewMatrix());
        Matrix4 modelMatrix=new Matrix4(getModelMatrix());
        //Matrix4 modelMatrix=new Matrix4();

        //mvp=projectionMatrix.multiply(viewMatrix.multiply(modelMatrix));
        mvp=modelMatrix.multiply(viewMatrix.multiply(projectionMatrix));

        shaderActionSequence.display(gl);

        //logger.info("gl_display_count="+gl_display_count);
        gl_display_count++;
    }

    public double glUnitsPerPixel() {
        return Math.abs( camera.getCameraFocusDistance() ) / DISTANCE_TO_SCREEN_IN_PIXELS;
    }

    public void reshape(GL4 gl, int x, int y, int width, int height) {
        viewport.setHeightPixels(height);
        viewport.setWidthPixels(width);
        display(gl);
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

        double cameraFocusDistance = (double)camera.getCameraFocusDistance();
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

        double sceneUnitsPerViewportHeight=cameraFocusDistance*Math.tan(0.5*camera.getFovRadians())*2.0;

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
