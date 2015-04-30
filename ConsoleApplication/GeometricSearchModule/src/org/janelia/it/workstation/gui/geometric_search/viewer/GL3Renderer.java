package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.UnitVec3;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.geometric_search.gl.GL3ShaderActionSequence;
import org.janelia.it.workstation.gui.geometric_search.gl.GL3SimpleActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 4/10/15.
 */
public class GL3Renderer implements GLEventListener
{
    public static final double DISTANCE_TO_SCREEN_IN_PIXELS = 2000;

    protected GLU glu = new GLU();
    protected List<GL3ShaderActionSequence> shaderActionList = new ArrayList<GL3ShaderActionSequence>();
    protected Color backgroundColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    protected Camera3d camera;

    public static final double MAX_PIXELS_PER_VOXEL = 100000.0;
    public static final double MIN_PIXELS_PER_VOXEL = 0.001;
    private static final double MIN_CAMERA_FOCUS_DISTANCE = -1000000.0;
    private static final double MAX_CAMERA_FOCUS_DISTANCE = -0.001;
    private static final Vec3 UP_IN_CAMERA = new Vec3(0,-1,0);
    private static double FOV_Y_DEGREES = 45.0f;
    private float FOV_TERM = new Float(Math.tan( (Math.PI/180.0) * (FOV_Y_DEGREES/2.0) ) );


    // camera parameters
    private double defaultHeightInPixels = 400.0;
    private double widthInPixels = defaultHeightInPixels;
    private double heightInPixels = defaultHeightInPixels;
    private GL3Model model;
    private boolean resetFirstRedraw;
    private boolean hasBeenReset = false;

    Matrix4 viewMatrix;
    Matrix4 projectionMatrix;

    private Logger logger = LoggerFactory.getLogger(GL3Renderer.class);

    public Matrix4 getViewMatrix() {
        return viewMatrix;
    }

    public Matrix4 getProjectionMatrix() {
        return projectionMatrix;
    }

    // scene objects
    public GL3Renderer(GL3Model model) {
        this.model=model;
        camera=model.getCamera3d();
    }

    public void addShaderAction(GL3ShaderActionSequence shaderAction) {
        shaderActionList.add(shaderAction);
    }

    protected void displayBackground(GL3 gl)
    {
        // paint solid background color
        gl.glClearColor(
                backgroundColor.getRed()/255.0f,
                backgroundColor.getGreen()/255.0f,
                backgroundColor.getBlue()/255.0f,
                backgroundColor.getAlpha()/255.0f);
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT);
    }

    public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged)
    {
        // System.out.println("displayChanged called");
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable)
    {
        final GL3 gl = glDrawable.getGL().getGL3();

        for (GL3ShaderActionSequence shaderAction : shaderActionList)
            shaderAction.dispose(gl);
    }

    public List<GL3ShaderActionSequence> getShaderActionList() {
        return shaderActionList;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Camera3d getCamera() {
        return camera;
    }

    @Override
    public void init(GLAutoDrawable glDrawable)
    {
        final GL3 gl = glDrawable.getGL().getGL3();

        for (GL3ShaderActionSequence shaderAction : shaderActionList) {
            try {
                shaderAction.init(gl);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setCamera(Camera3d camera) {
        this.camera = camera;
    }


    public void centerOnPixel(Point p) {
        double dx =  p.x - widthInPixels/2.0;
        double dy = heightInPixels/2.0 - p.y;
        translatePixels(-dx, dy, 0.0);
    }

    public void clear() {
        shaderActionList.clear();
        hasBeenReset = false;
    }

    public void requestReset() {
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {

        // Preset background from the volume model.
        float[] backgroundClrArr = model.getBackgroundColorFArr();
        this.backgroundColor = new Color( backgroundClrArr[ 0 ], backgroundClrArr[ 1 ], backgroundClrArr[ 2 ] );

        final GL3 gl = glDrawable.getGL().getGL3();
        displayBackground(gl);

        widthInPixels = glDrawable.getWidth();
        heightInPixels = glDrawable.getHeight();
        if (resetFirstRedraw && (! hasBeenReset)) {
            resetView();
            hasBeenReset = true;
        }

        // View matrix
        Vec3 f = camera.getFocus(); // The focus point
        Rotation3d rotation = camera.getRotation();
        double unitsPerPixel = glUnitsPerPixel();

        //Vec3 c = f.plus(rotation.times(model.getCameraDepth().times(unitsPerPixel))); // camera position

        Vec3 c = f.plus(rotation.times(model.getCameraDepth())); // camera position

        Vec3 u = rotation.times(UP_IN_CAMERA); // up vector

        Vector3 f3 = new Vector3(new Float(f.getX()), new Float(f.getY()), new Float(f.getZ()));
        Vector3 c3 = new Vector3(new Float(c.getX()), new Float(c.getY()), new Float(c.getZ()));
        Vector3 u3 = new Vector3(new Float(u.getX()), new Float(u.getY()), new Float(u.getZ()));

        viewMatrix = lookAt(c3, f3, u3);

        // Projection matrix
        updateProjection(gl);

        // Copy member list of actors local for independent iteration.
        for (GL3ShaderActionSequence shaderAction : shaderActionList)
            shaderAction.display(gl);

    }

    public double glUnitsPerPixel() {
        return Math.abs( model.getCameraFocusDistance() ) / DISTANCE_TO_SCREEN_IN_PIXELS;
    }

    public void resetView() {
        // Adjust view to fit the actual objects present
//        BoundingBox3d boundingBox = getBoundingBox();
//        camera.setFocus(boundingBox.getCenter());
        camera.resetRotation();
        camera.setFocus(0.0, 0.0, 0.0);
        model.setCameraDepth(new Vec3(0.0, 0.0, -GL3Model.DEFAULT_CAMERA_FOCUS_DISTANCE));
//        resetCameraDepth(boundingBox);
    }

    @Override
    public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
        this.widthInPixels = width;
        this.heightInPixels = height;

        final GL3 gl = glDrawable.getGL().getGL3();

        updateProjection(gl);
    }


    public void rotatePixels(double dx, double dy, double dz) {
        // Rotate like a trackball
        double dragDistance = Math.sqrt(dy * dy + dx * dx + dz * dz);
        if (dragDistance <= 0.0)
            return;
        UnitVec3 rotationAxis = new UnitVec3(dy, -dx, dz);
        double windowSize = Math.sqrt(
                widthInPixels * widthInPixels
                        + heightInPixels * heightInPixels);
        // Drag across the entire window to rotate all the way around
        double rotationAngle = 2.0 * Math.PI * dragDistance/windowSize;
        // System.out.println(rotationAxis.toString() + rotationAngle);
        Rotation3d rotation = new Rotation3d().setFromAngleAboutUnitVector(
                rotationAngle, rotationAxis);
        // System.out.println(rotation);
        camera.setRotation(camera.getRotation().times(rotation.transpose()));
        // System.out.println(R_ground_camera);
    }

    public void translatePixels(double dx, double dy, double dz) {
        // trackball translate
        Vec3 t = new Vec3(-dx, -dy, -dz);
        model.getCamera3d().getFocus().plusEquals(
                camera.getRotation().times(t)
        );
    }

    public void updateProjection(GL3 gl) {
        //gl.glViewport(0, 0, (int) widthInPixels, (int) heightInPixels);
        final float h = (float) widthInPixels / (float) heightInPixels;
        double cameraFocusDistance = model.getCameraFocusDistance();
        float scaledFocusDistance = new Float(Math.abs(cameraFocusDistance));
        projectionMatrix = computeProjection(h, 0.5f * scaledFocusDistance, 2.0f * scaledFocusDistance);
    }

    Matrix4 computeProjection(float aspectRatio, float near, float far) {
        Matrix4 projection = new Matrix4();
        float top=near*FOV_TERM;
        float bottom = -1f * top;
        float right = top * aspectRatio;
        float left = -1f * right;

        projection.set(

                (2f * near) / (right - left),     0f,                             (right + left) / (right - left),         0f,
                0f,                               (2f * near) / (top - bottom),   (top + bottom) / (top - bottom),         0f,
                0f,                               0f,                             -1f * ((far + near) / (far - near)),    -1f * ((2f * far * near) / (far - near)),
                0f,                               0f,                             -1f,                                     0f);

        return projection;
    }

    public void zoom(double zoomRatio) {
        if (zoomRatio <= 0.0) {
            return;
        }
        if (zoomRatio == 1.0) {
            return;
        }

        double cameraFocusDistance = model.getCameraFocusDistance();
        cameraFocusDistance /= zoomRatio;
        if ( cameraFocusDistance > MAX_CAMERA_FOCUS_DISTANCE ) {
            return;
        }
        if ( cameraFocusDistance < MIN_CAMERA_FOCUS_DISTANCE ) {
            return;
        }
        model.setCameraPixelsPerSceneUnit(DISTANCE_TO_SCREEN_IN_PIXELS, cameraFocusDistance);

        model.setCameraDepth(new Vec3(0.0, 0.0, cameraFocusDistance));

    }

    public void zoomPixels(Point newPoint, Point oldPoint) {
        // Are we dragging away from the center, or toward the center?
        double[] p0 = {oldPoint.x - widthInPixels/2.0,
                oldPoint.y - heightInPixels/2.0};
        double[] p1 = {newPoint.x - widthInPixels/2.0,
                newPoint.y - heightInPixels/2.0};
        double dC0 = Math.sqrt(p0[0]*p0[0]+p0[1]*p0[1]);
        double dC1 = Math.sqrt(p1[0]*p1[0]+p1[1]*p1[1]);
        double dC = dC1 - dC0; // positive means away
        double denom = Math.max(20.0, dC1);
        double zoomRatio = 1.0 + dC/denom;
        zoom(zoomRatio);
    }

    public GL3Model getModel() {
        return model;
    }

    private double maxAspectRatio(BoundingBox3d boundingBox) {

        double boundingAspectRatio = Math.max(
                boundingBox.getWidth() / boundingBox.getHeight(), boundingBox.getHeight() / boundingBox.getWidth()
        );
        boolean horizontalBox = boundingBox.getWidth() > boundingBox.getHeight();

        double glAspectRatio = Math.max(
                widthInPixels / heightInPixels, heightInPixels / widthInPixels
        );
        boolean horizontalGl = widthInPixels > heightInPixels;

        if ( horizontalGl && horizontalBox ) {
            return Math.max(
                    boundingAspectRatio, glAspectRatio
            );

        }
        else {
            return boundingAspectRatio * glAspectRatio;
        }

    }

    public void setResetFirstRedraw(boolean resetFirstRedraw) {
        this.resetFirstRedraw = resetFirstRedraw;
    }

    private double getMaxZoom() {
        double maxRes = getMaxRes();
        return MAX_PIXELS_PER_VOXEL / maxRes; // This many pixels per voxel is probably zoomed enough...
    }

    private double getMaxRes() {
        return (double) Math.min(
                model.getVoxelMicrometers()[0],
                Math.min(
                        model.getVoxelMicrometers()[1],
                        model.getVoxelMicrometers()[2]
                )
        );
    }

    // c = camera position
    // f = focus point
    // u = up vector
    protected Matrix4 lookAt(Vector3 c, Vector3 f, Vector3 u) {
        Vector3 forward = new Vector3(f.getX() - c.getX(), f.getY() - c.getY(), f.getZ() - c.getZ());
        Vector3 fn=forward.normalize();
        Vector3 side=computeNormalOfPlane(fn, u);
        Vector3 sn=side.normalize();
        Vector3 up=computeNormalOfPlane(sn, fn);
        Matrix4 lam = new Matrix4();

        lam.set( sn.getX(),   up.getX(),    fn.getX(),   0.0f,
                 sn.getY(),   up.getY(),    fn.getY(),   0.0f,
                 sn.getZ(),   up.getZ(),    fn.getZ(),   0.0f,
                 0.0f,         0.0f,        0.0f,        1.0f);

        Matrix4 tm = new Matrix4();

        tm.set( 1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                -1f*c.getX(), -1f*c.getY(), -1f*c.getZ(), 1f);

        Matrix4 result = lam.multiply(tm);
        return result;
    }

    protected Vector3 computeNormalOfPlane(Vector3 v1, Vector3 v2) {
        Vector3 norm=new Vector3(v1.getY() * v2.getZ() - v1.getZ() * v2.getY(),
                                 v1.getZ() * v2.getX() - v1.getX() * v2.getZ(),
                                 v1.getX() * v2.getY() - v1.getY() * v2.getX());
        return norm;
    }


}
