package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.UnitVec3;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4ShaderActionSequence;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4SimpleActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import java.awt.*;
import java.util.Deque;

/**
 * Created by murphys on 4/10/15.
 */
public class VoxelViewerRenderer implements GLEventListener
{
    private Logger logger = LoggerFactory.getLogger(VoxelViewerRenderer.class);

    protected GLU glu = new GLU();

    private VoxelViewerModel model;
    protected Camera3d camera;
    VoxelViewerProperties properties;
    VoxelViewerGLPanel viewer;

    protected ArrayTransparencyContext atc = new ArrayTransparencyContext();
    protected ArraySortShader ass = new ArraySortShader(null);
    protected GL4ShaderActionSequence assActionSequence = new GL4ShaderActionSequence("Sort Shader");

    protected Color backgroundColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);

    public static final double DISTANCE_TO_SCREEN_IN_PIXELS = 2000;
    private static final double MAX_CAMERA_FOCUS_DISTANCE = 1000000.0;
    private static final double MIN_CAMERA_FOCUS_DISTANCE = 0.001;
    private static final Vec3 UP_IN_CAMERA = new Vec3(0,1,0);
    private static double FOV_Y_DEGREES = 45.0f; 
    private float FOV_TERM = new Float(Math.tan( (Math.PI/180.0) * (FOV_Y_DEGREES/2.0) ) );

    // camera parameters
    private double widthInPixels = 1200.0;
    private double heightInPixels = 800.0;
    private boolean resetFirstRedraw;
    private boolean hasBeenReset = false;

    Matrix4 viewMatrix;
    Matrix4 projectionMatrix;


    public Matrix4 getViewMatrix() {
        return viewMatrix;
    }

    public Matrix4 getProjectionMatrix() {
        return projectionMatrix;
    }

    public void setProperties(VoxelViewerProperties properties) {
        this.properties=properties;
        ass.setProperties(properties);
    }

    // scene objects
    public VoxelViewerRenderer(VoxelViewerModel model) {
        this.model=model;
        camera=model.getCamera3d();
    }

    public void setViewer(VoxelViewerGLPanel viewer) {
        this.viewer=viewer;
    }

    public void setPixelDimensions(double widthInPixels, double heightInPixels) {
        this.widthInPixels=widthInPixels;
        this.heightInPixels=heightInPixels;
    }

    protected void displayBackground(GL4 gl)
    {
        // paint solid background color
        gl.glClearColor(
                backgroundColor.getRed(),
                backgroundColor.getGreen(),
                backgroundColor.getBlue(),
                backgroundColor.getAlpha());
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT);
    }

//    public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged)
//    {
//        // System.out.println("displayChanged called");
//    }

    @Override
    public void dispose(GLAutoDrawable glDrawable)
    {
        final GL4 gl = glDrawable.getGL().getGL4();
        model.disposeAndClearAll(gl);
        ass.dispose(gl);
        atc.dispose(gl);
    }

//    public Color getBackgroundColor() {
//        return backgroundColor;
//    }

    public Camera3d getCamera() {
        return camera;
    }

    @Override
    public void init(GLAutoDrawable glDrawable)
    {
        final GL4 gl = glDrawable.getGL().getGL4();
        
        try {

            // Array transparency context
            atc.setWidth(glDrawable.getWidth());
            atc.setHeight(glDrawable.getHeight());
            int depth=0;
            if (properties==null) {
                logger.error("VoxelViewerProperties is null - cannot set transparency depth");
            } else {
                depth=properties.getInteger(VoxelViewerProperties.GL_TRANSPARENCY_QUARTERDEPTH_INT);
            }
            
            logger.info("atc init()");
            
            atc.setTransparencyQuarterDepth(depth);
            atc.init(gl);
            
            logger.info("ass init()");

            ass.setProperties(properties);
            ass.setTransparencyContext(atc);
            final int transparencyQuarterDepth=model.getTransparencyQuarterDepth();
            ass.setUpdateCallback(new GLDisplayUpdateCallback() {
                @Override
                public void update(GL4 gl) {
                    int width=viewer.getWidth();
                    int height=viewer.getHeight();
                    logger.info("Calling sortShader.set* with depth="+transparencyQuarterDepth+" width="+width+" height="+height);
                    ass.setWidth(gl, viewer.getWidth());
                    ass.setHeight(gl, viewer.getHeight());
                    ass.setDepth(gl, transparencyQuarterDepth);
                }
            });
            assActionSequence.setShader(ass);
            assActionSequence.setApplyMemoryBarrier(true);
            assActionSequence.init(gl);

            model.initAll(atc, gl);

            logger.info("Done with init()");

        } catch (Exception ex) {
            ex.printStackTrace();
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
        model.setDisposeAndClearAllActorsMsg();
    }

    public void requestReset() {
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {

        logger.info("display() start");

        final GL4 gl = glDrawable.getGL().getGL4();

        logger.info("display() check 1");

        // First, handle messages
        Deque<String> msgQueue=model.getMessageQueue();
        while (msgQueue.size()>0) {
            String msg=msgQueue.pop();
            if (msg.equals(VoxelViewerModel.DISPOSE_AND_CLEAR_ALL_ACTORS_MSG)) {
                model.disposeAndClearAllActors(gl);
            }
        }

        logger.info("display() check 2");

        // Next, handle any inits
        Deque<GL4SimpleActor> initQueue=model.getInitQueue();
        while (initQueue.size()>0) {
            GL4SimpleActor actor = initQueue.pop();
            actor.init(gl);
            if (actor instanceof ArrayCubeGLActor) {
                model.getDenseVolumeShaderActionSequence().getActorSequence().add(actor);
            } else if (actor instanceof ArrayMeshGLActor) {
                model.getMeshShaderActionSequence().getActorSequence().add(actor);
            }
        }

        logger.info("display() check 3");

        // Last, handle any removals
        Deque<Integer> removalQueue=model.getDisposeQueue();
        while (removalQueue.size()>0) {
            Integer actorIdToRemove=removalQueue.pop();
            model.removeActor(actorIdToRemove, gl);
        }

        logger.info("display() check 4");

        // NOTE: THIS STUFF SHOULD BE HANDLED IN THE SHADERS
        // Preset background from the volume model.
        float[] backgroundClrArr = model.getBackgroundColorFArr();
        this.backgroundColor = new Color( backgroundClrArr[ 0 ], backgroundClrArr[ 1 ], backgroundClrArr[ 2 ] );

        //displayBackground(gl);
        gl.glClear(GL4.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL4.GL_DEPTH_TEST);

        widthInPixels = glDrawable.getWidth();
        heightInPixels = glDrawable.getHeight();
        if (resetFirstRedraw && (! hasBeenReset)) {
            //resetView();
            hasBeenReset = true;
        }

        // View matrix
        Vec3 f = camera.getFocus(); // The focus point
        Rotation3d rotation = camera.getRotation();
        double unitsPerPixel = glUnitsPerPixel();

        //Vec3 c = f.plus(rotation.times(model.getCameraDepth().times(unitsPerPixel))); // camera position

        Vec3 md = model.getCameraDepth();

        Vec3 c = f.plus(rotation.times(md)); // camera position

        Vec3 u = rotation.times(UP_IN_CAMERA); // up vector

        Vector3 f3 = new Vector3(new Float(f.getX()), new Float(f.getY()), new Float(f.getZ()));
        Vector3 c3 = new Vector3(new Float(c.getX()), new Float(c.getY()), new Float(c.getZ()));
        Vector3 u3 = new Vector3(new Float(u.getX()), new Float(u.getY()), new Float(u.getZ()));

//        logger.info("camera= "+c3.getX()+" "+c3.getY()+" "+c3.getZ());
//        logger.info("focus=  "+f3.getX()+" "+f3.getY()+" "+f3.getZ());
//        logger.info("up=     "+u3.getX()+" "+u3.getY()+" "+u3.getZ());

        viewMatrix = lookAt(c3, f3, u3);

        // Projection matrix
        updateProjection(gl);

        // Copy member list of actors local for independent iteration.
        //logger.info("Display shader sequence starting");

        logger.info("display() check 5");

        if (model.getDenseVolumeShaderActionSequence().getActorSequence().size()>0 ||
                model.getMeshShaderActionSequence().getActorSequence().size()>0) {

            try {
                logger.info("display() check 6");

                atc.display(gl);

                logger.info("display() check 7");


            }
            catch (Exception ex) {
                logger.error("Error in ArrayTransparencyContext display(): " + ex.getMessage());
                ex.printStackTrace();
            }

            logger.info("display() check 8");

            model.getDenseVolumeShaderActionSequence().display(gl);

            logger.info("display() check 9");

            model.getMeshShaderActionSequence().display(gl);

            logger.info("display() check 10");

            assActionSequence.display(gl);
        }

        logger.info("display() check 11");

    }

    public double glUnitsPerPixel() {
        return Math.abs( model.getCameraFocusDistance() ) / DISTANCE_TO_SCREEN_IN_PIXELS;
    }

    public void resetView() {
        camera.resetRotation();
        camera.setFocus(0.0, 0.0, 0.5);
        model.setCameraDepth(new Vec3(0.0, 0.0, VoxelViewerModel.DEFAULT_CAMERA_FOCUS_DISTANCE));
    }

    @Override
    public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
        this.widthInPixels = width;
        this.heightInPixels = height;

        final GL4 gl = glDrawable.getGL().getGL4();

        updateProjection(gl);
    }


    public void rotatePixels(double dx, double dy, double dz) {
        // Rotate like a trackball
        double dragDistance = Math.sqrt(dy * dy + dx * dx + dz * dz);
        if (dragDistance <= 0.0)
            return;
        UnitVec3 rotationAxis = new UnitVec3(dy, dx, dz);
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
        Vec3 t = new Vec3(-dx, dy, -dz);
        t.multEquals(glUnitsPerPixel());
        model.getCamera3d().getFocus().plusEquals(
                camera.getRotation().times(t)
        );
    }

    public void updateProjection(GL4 gl) {
        //gl.glViewport(0, 0, (int) widthInPixels, (int) heightInPixels);
        
        //logger.info("updateProjection() using widthInPixels="+widthInPixels+" heightInPixels="+heightInPixels);
        
        final float h = (float) widthInPixels / (float) heightInPixels;
        double cameraFocusDistance = model.getCameraFocusDistance();
        float scaledFocusDistance = new Float(Math.abs(cameraFocusDistance));
        //projectionMatrix = computeProjection(h, 0.5f*scaledFocusDistance, 2.0f*scaledFocusDistance);
        projectionMatrix = computeProjection(h, 0.01f*scaledFocusDistance, 2.0f*scaledFocusDistance);
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
        projection.transpose();

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

//        logger.info("ZOOM  glUnitsPerPixel=" + glUnitsPerPixel() + " cameraFocusDistance=" + cameraFocusDistance);

    }

    public void zoomPixels(Point newPoint, Point oldPoint) {
        // Are we dragging away from the center, or toward the center?
        double[] p0 = {oldPoint.x - widthInPixels/2.0,
                oldPoint.y - heightInPixels/2.0};
        double[] p1 = {newPoint.x - widthInPixels/2.0,
                newPoint.y - heightInPixels/2.0};
        double dC0 = Math.sqrt(p0[0] * p0[0] + p0[1] * p0[1]);
        double dC1 = Math.sqrt(p1[0]*p1[0]+p1[1]*p1[1]);
        double dC = dC1 - dC0; // positive means away
        double denom = Math.max(20.0, dC1);
        double zoomRatio = 1.0 + dC/denom;
        zoom(zoomRatio);
    }

    public VoxelViewerModel getModel() {
        return model;
    }


    public void setResetFirstRedraw(boolean resetFirstRedraw) {
        this.resetFirstRedraw = resetFirstRedraw;
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

        lam.set( sn.getX(),   up.getX(),    -1f*fn.getX(),   0.0f,
                 sn.getY(),   up.getY(),    -1f*fn.getY(),   0.0f,
                 sn.getZ(),   up.getZ(),    -1f*fn.getZ(),   0.0f,
                 0.0f,         0.0f,        0.0f,        1.0f);

        Matrix4 tm = new Matrix4();

        tm.set( 1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                -1f*c.getX(), -1f*c.getY(), -1f*c.getZ(), 1f);

        Matrix4 result = tm.multiply(lam);
        return result;
    }

    protected Vector3 computeNormalOfPlane(Vector3 v1, Vector3 v2) {
        Vector3 norm=new Vector3(v1.getY() * v2.getZ() - v1.getZ() * v2.getY(),
                                 v1.getZ() * v2.getX() - v1.getX() * v2.getZ(),
                                 v1.getX() * v2.getY() - v1.getY() * v2.getX());
        return norm;
    }


}
