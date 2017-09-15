package org.janelia.it.workstation.ab2.renderer;

import java.awt.Point;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.ab2.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.gui.camera.Camera3d;

public class AB2Basic3DRenderer extends AB23DRenderer {

    protected Camera3d camera3d;
    private Vec3 cameraDepth;
    public static final double DEFAULT_CAMERA_FOCUS_DISTANCE = 2.0;

    Vector4 backgroundColor=new Vector4(0.0f, 0.0f, 0.0f, 0.0f);

    int blend_method=0; // 0=transparency, 1=mip

    public static final int BLEND_METHOD_MIX=0;
    public static final int BLEND_METHOD_MIP=1;

    public static final double DISTANCE_TO_SCREEN_IN_PIXELS = 2000;
    private static final double MAX_CAMERA_FOCUS_DISTANCE = 1000000.0;
    private static final double MIN_CAMERA_FOCUS_DISTANCE = 0.001;
    private static final Vec3 UP_IN_CAMERA = new Vec3(0,1,0);
    private static double FOV_Y_DEGREES = 45.0f;
    private float FOV_TERM = new Float(Math.tan( (Math.PI/180.0) * (FOV_Y_DEGREES/2.0) ) );

    // camera parameters
    private double widthInPixels = 1000.0; // todo: these need to come from gui status
    private double heightInPixels = 750.0;
    private boolean resetFirstRedraw;
    private boolean hasBeenReset = false;

    Matrix4 viewMatrix;
    Matrix4 projectionMatrix;

    GLShaderActionSequence shaderActionSequence=new GLShaderActionSequence(AB2Basic3DRenderer.class.getName());
    GLShaderProgram shader;


    public Matrix4 getViewMatrix() {
        return viewMatrix;
    }

    public Matrix4 getProjectionMatrix() {
        return projectionMatrix;
    }


    public AB2Basic3DRenderer() {
        camera3d = new BasicObservableCamera3d();
        camera3d.setFocus(0.0,0.0,0.5);
        cameraDepth = new Vec3(0.0, 0.0, DEFAULT_CAMERA_FOCUS_DISTANCE);
    }

    public void setPixelDimensions(double widthInPixels, double heightInPixels) {
        this.widthInPixels=widthInPixels;
        this.heightInPixels=heightInPixels;
    }

    public void centerOnPixel(Point p) {
        double dx =  p.x - widthInPixels/2.0;
        double dy = heightInPixels/2.0 - p.y;
        translatePixels(-dx, dy, 0.0);
    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        initSync(glAutoDrawable);
    }

    private synchronized void initSync(GLAutoDrawable glDrawable) {
        final GL4 gl = glDrawable.getGL().getGL4();

        // need to create and load shader here

        try {
            shader.setUpdateCallback(new GLDisplayUpdateCallback() {
                @Override
                public void update(GL4 gl) {

                }
            });
            shaderActionSequence.setShader(shader);
            shaderActionSequence.setApplyMemoryBarrier(false);
            shaderActionSequence.init(gl);

            // todo: init model actors?

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        final GL4 gl = glAutoDrawable.getGL().getGL4();
        shaderActionSequence.dispose(gl);
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {

    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {

    }

    public void rotatePixels(double dx, double dy, double dz) {

    }

    public void translatePixels(double dx, double dy, double dz) {

    }

    public void zoomPixels(Point newPoint, Point oldPoint) {

    }

}
