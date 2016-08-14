package org.janelia.it.workstation.gui.viewer3d;

import org.janelia.it.jacs.shared.geom.Rotation3d;
import org.janelia.it.jacs.shared.geom.UnitVec3;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.gui.opengl.GL2Adapter;
import org.janelia.it.workstation.gui.opengl.GL2AdapterFactory;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GLAutoDrawable;
import java.awt.*;

public abstract class ActorRenderer 
    extends BaseRenderer
{
    public static final double MAX_PIXELS_PER_VOXEL = 100.0;
    public static final double MIN_PIXELS_PER_VOXEL = 0.001;
    private static final double DEFAULT_CAMERA_FOCUS_DISTANCE = 20.0;
    private static final double MIN_CAMERA_FOCUS_DISTANCE = -100000.0;
    private static final double MAX_CAMERA_FOCUS_DISTANCE = -0.001;
    protected static final Vec3 UP_IN_CAMERA = new Vec3(0,-1,0);

    // camera parameters
    private double defaultHeightInPixels = 400.0;
    private double widthInPixels = defaultHeightInPixels;
    private double heightInPixels = defaultHeightInPixels;
    private VolumeModel volumeModel;
    private boolean resetFirstRedraw;
    private boolean hasBeenReset = false;

    private Logger logger = LoggerFactory.getLogger(ActorRenderer.class);

    // scene objects
    public ActorRenderer() {
		// actors.add(new TeapotActor()); // solid shading is not supported right now
        this(new VolumeModel());
        initializeCamera();
    }

    public ActorRenderer( VolumeModel volumeModel ) {
        if (volumeModel.getCamera3d() == null) {
            initializeCamera();
        }
        this.volumeModel = volumeModel;
    }
    
    public void centerOnPixel(Point p) {
    		// System.out.println("center");
    		double dx =  p.x - getWidthInPixels()/2.0;
    		double dy = getHeightInPixels()/2.0 - p.y;
    		translatePixels(-dx, dy, 0.0);
    }
    
    public void clear() {
		actors.clear();
        hasBeenReset = false;
    }

    public void requestReset() {

    }

    public double glUnitsPerPixel() {
        return Math.abs( getVolumeModel().getCameraFocusDistance() ) / DISTANCE_TO_SCREEN_IN_PIXELS;
    }

    public void resetView() {
        // Adjust view to fit the actual objects present
        org.janelia.it.jacs.shared.viewer3d.BoundingBox3d boundingBox = getBoundingBox();
        getVolumeModel().getCamera3d().setFocus(boundingBox.getCenter());
        getVolumeModel().getCamera3d().resetRotation();
        resetCameraDepth(boundingBox);
    }

    @Override
    public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
        this.setWidthInPixels(width);
        this.setHeightInPixels(height);

        // System.out.println("reshape() called: x = "+x+", y = "+y+", width = "+width+", height = "+height);
        //final GL2 gl = glDrawable.getGL().getGL2();
        GL2Adapter gl2Adapter = GL2AdapterFactory.createGL2Adapter( glDrawable );
 
        updateProjection(gl2Adapter);
        gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
        gl2Adapter.glLoadIdentity();

        double previousFocusDistance = getVolumeModel().getCameraFocusDistance();
        if ( previousFocusDistance == DEFAULT_CAMERA_FOCUS_DISTANCE ) {
            org.janelia.it.jacs.shared.viewer3d.BoundingBox3d boundingBox = getBoundingBox();
            resetCameraDepth(boundingBox);
        }
    }

	public void rotatePixels(double dx, double dy, double dz) {
		// Rotate like a trackball
		double dragDistance = Math.sqrt(dy*dy + dx*dx + dz*dz);
		if (dragDistance <= 0.0)
			return;
		UnitVec3 rotationAxis = new UnitVec3(dy, -dx, dz);
		double windowSize = Math.sqrt(
				getWidthInPixels()*getWidthInPixels() 
				+ getHeightInPixels()*getHeightInPixels());
		// Drag across the entire window to rotate all the way around
		double rotationAngle = 2.0 * Math.PI * dragDistance/windowSize;
		// System.out.println(rotationAxis.toString() + rotationAngle);
		Rotation3d rotation = new Rotation3d().setFromAngleAboutUnitVector(
				rotationAngle, rotationAxis);
		// System.out.println(rotation);
        getVolumeModel().getCamera3d().setRotation( getVolumeModel().getCamera3d().getRotation().times( rotation.transpose() ) );
		// System.out.println(R_ground_camera);
	}

	public void translatePixels(double dx, double dy, double dz) {
		// trackball translate
		Vec3 t = new Vec3(-dx, -dy, -dz);
		getVolumeModel().getCamera3d().getFocus().plusEquals(
                getVolumeModel().getCamera3d().getRotation().times(t)
        );
	}
	
	public void updateProjection(GL2Adapter gl) {
        gl.getGL2GL3().glViewport(0, 0, (int) getWidthInPixels(), (int) getHeightInPixels());
        double verticalApertureInDegrees = 180.0/Math.PI * 2.0 * Math.abs(
        		Math.atan2(getHeightInPixels()/2.0, DISTANCE_TO_SCREEN_IN_PIXELS));
        gl.glMatrixMode( GL2Adapter.MatrixMode.GL_PROJECTION );
        gl.glLoadIdentity();
        final float h = (float) getWidthInPixels() / (float) getHeightInPixels();
        double cameraFocusDistance = getVolumeModel().getCameraFocusDistance();
        double scaledFocusDistance = Math.abs(cameraFocusDistance) * glUnitsPerPixel();
        glu.gluPerspective(verticalApertureInDegrees,
        		h,
        		0.5 * scaledFocusDistance,
        		2.0 * scaledFocusDistance);
	}
	
	public void zoom(double zoomRatio) {
		if (zoomRatio <= 0.0) {
			return;
        }
		if (zoomRatio == 1.0) {
			return;
        }

        double cameraFocusDistance = getVolumeModel().getCameraFocusDistance();
        cameraFocusDistance /= zoomRatio;
        if ( cameraFocusDistance > MAX_CAMERA_FOCUS_DISTANCE ) {
            return;
        }
        if ( cameraFocusDistance < MIN_CAMERA_FOCUS_DISTANCE ) {
            return;
        }
        getVolumeModel().setCameraPixelsPerSceneUnit( DISTANCE_TO_SCREEN_IN_PIXELS, cameraFocusDistance );

        getVolumeModel().setCameraDepth(new Vec3(0.0, 0.0, cameraFocusDistance));

    }
	
	public void zoomPixels(Point newPoint, Point oldPoint) {
		// Are we dragging away from the center, or toward the center?
		double[] p0 = {oldPoint.x - getWidthInPixels()/2.0,
				oldPoint.y - getHeightInPixels()/2.0};
		double[] p1 = {newPoint.x - getWidthInPixels()/2.0,
				newPoint.y - getHeightInPixels()/2.0};
		double dC0 = Math.sqrt(p0[0]*p0[0]+p0[1]*p0[1]);
		double dC1 = Math.sqrt(p1[0]*p1[0]+p1[1]*p1[1]);
		double dC = dC1 - dC0; // positive means away
		double denom = Math.max(20.0, dC1);
		double zoomRatio = 1.0 + dC/denom;
		zoom(zoomRatio);
    }

    public final VolumeModel getVolumeModel() {
        return volumeModel;
    }
    
    /** Optionally override the default volume model. */
    public void setVolumeModel(VolumeModel volumeModel) {
        this.volumeModel = volumeModel;
    }

    protected void resetOnFirstRedraw() {
        if (resetFirstRedraw && (! hasBeenReset)) {
            resetView();
            hasBeenReset = true;
        }
    }
 
    /**
     * @return the widthInPixels
     */
    protected double getWidthInPixels() {
        return widthInPixels;
    }

    /**
     * @return the heightInPixels
     */
    protected double getHeightInPixels() {
        return heightInPixels;
    }

    /**
     * @param widthInPixels the widthInPixels to set
     */
    protected void setWidthInPixels(double widthInPixels) {
        this.widthInPixels = widthInPixels;
    }

    /**
     * @param heightInPixels the heightInPixels to set
     */
    protected void setHeightInPixels(double heightInPixels) {
        this.heightInPixels = heightInPixels;
    }

    private double maxAspectRatio(org.janelia.it.jacs.shared.viewer3d.BoundingBox3d boundingBox) {

        double boundingAspectRatio = Math.max(
                boundingBox.getWidth() / boundingBox.getHeight(), boundingBox.getHeight() / boundingBox.getWidth()
        );
        boolean horizontalBox = boundingBox.getWidth() > boundingBox.getHeight();

        double glAspectRatio = Math.max(
                getWidthInPixels() / getHeightInPixels(), getHeightInPixels() / getWidthInPixels()
        );
        boolean horizontalGl = getWidthInPixels() > getHeightInPixels();

        if ( horizontalGl && horizontalBox ) {
            return Math.max(
                    boundingAspectRatio, glAspectRatio
            );

        }
        else {
            return boundingAspectRatio * glAspectRatio;
        }

    }

    protected void resetCameraDepth(org.janelia.it.jacs.shared.viewer3d.BoundingBox3d boundingBox) {
        double heightInMicrometers = boundingBox.getHeight();
        if (heightInMicrometers <= 0.0) { // watch for NaN!
            logger.warn("Adjusted height to account for zero-height bounding box.");
            heightInMicrometers = 2.0; // whatever
        }
        // System.out.println("Focus = " + focusInGround);
        // cameraFocusDistance = DEFAULT_CAMERA_FOCUS_DISTANCE * defaultHeightInPixels / heightInPixels;
        double finalAspectRatio = maxAspectRatio(boundingBox);
        double heightRatioFactor = heightInMicrometers / getHeightInPixels();
        if ( heightRatioFactor < 0.5 ) {
            heightRatioFactor *= (1.75 - heightRatioFactor) * (1.75 - heightRatioFactor);
        }
        else if ( heightRatioFactor > 1.5 ) {
            heightRatioFactor = 1.0;
        }
        double newFocusDistance = finalAspectRatio * 1.05 * DISTANCE_TO_SCREEN_IN_PIXELS * heightRatioFactor; 
        logger.debug("Setting camera depth to " + (-newFocusDistance) + " for finalAspectRatio of " + finalAspectRatio + " and hgithRatioFactor of " + heightRatioFactor);        
        getVolumeModel().setCameraDepth( new Vec3( 0.0, 0.0, -newFocusDistance ) );        
        getVolumeModel().setCameraPixelsPerSceneUnit(DISTANCE_TO_SCREEN_IN_PIXELS, getVolumeModel().getCameraFocusDistance());
    }

    private org.janelia.it.jacs.shared.viewer3d.BoundingBox3d getBoundingBox() {
        org.janelia.it.jacs.shared.viewer3d.BoundingBox3d boundingBox = new org.janelia.it.jacs.shared.viewer3d.BoundingBox3d();
        for (GLActor actor : actors) {
            boundingBox.include(actor.getBoundingBox3d());
        }
        if (boundingBox.isEmpty())
            boundingBox.include(new Vec3(0,0,0));
        return boundingBox;
    }

    public void setResetFirstRedraw(boolean resetFirstRedraw) {
        this.resetFirstRedraw = resetFirstRedraw;
    }

    private double getMaxZoom() {
        double maxRes = getMaxRes();
        return MAX_PIXELS_PER_VOXEL / maxRes; // This many pixels per voxel is probably zoomed enough...
    }

    private double getMaxRes() {
        VolumeModel model = getVolumeModel();
        return (double) Math.min(
            model.getVoxelMicrometers()[0],
            Math.min(
                model.getVoxelMicrometers()[1],
                model.getVoxelMicrometers()[2]
            )
        );
    }

    private void initializeCamera() {
        BasicObservableCamera3d camera3d = new BasicObservableCamera3d();
        camera3d.setFocus(0.0, 0.0, -DEFAULT_CAMERA_FOCUS_DISTANCE);
        getVolumeModel().setCamera3d(camera3d);
    }

    private double getMinZoom() {
        return MIN_PIXELS_PER_VOXEL / getMaxRes();
//        BoundingBox3d box = getBoundingBox();
//        Vec3 volSize = new Vec3(box.getWidth(), box.getHeight(), box.getDepth());
//
//        int w = getVolumeModel().getVoxelDimensions()[ 0 ];
//        int h = getVolumeModel().getVoxelDimensions()[ 1 ];
//        if (w > 0  &&  h > 0 ) {
//            // Fit two of the whole volume on the screen
//            // Rotate volume to match viewer orientation
////  Vec3 rotSize = viewer.getViewerInGround().inverse().times(volSize);
////            Vec3 rotSize = getVolumeModel().getCameraDepth().transpose().times(volSize);
//            Vec3 rotSize = getVolumeModel().getCamera3d().getRotation().inverse().times(volSize);
//            double zx = 0.5 * w / Math.abs(rotSize.x());
//            double zy = 0.5 * h / Math.abs(rotSize.y());
//            result =
//                Math.min(
//                    Math.min(zx, zy),
//                    result);
//        }
//        return result;
    }

}