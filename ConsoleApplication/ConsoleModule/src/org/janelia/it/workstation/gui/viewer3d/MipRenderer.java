package org.janelia.it.workstation.gui.viewer3d;

import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.UnitVec3;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.gui.opengl.GL2Adapter;
import org.janelia.it.workstation.gui.opengl.GL2AdapterFactory;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GLAutoDrawable;
import java.awt.*;
 
class MipRenderer 
    extends BaseRenderer
{
    public static final double MAX_PIXELS_PER_VOXEL = 100.0;
    public static final double MIN_PIXELS_PER_VOXEL = 0.001;
    private static final double DEFAULT_CAMERA_FOCUS_DISTANCE = 20.0;
    private static final double MIN_CAMERA_FOCUS_DISTANCE = -100000.0;
    private static final double MAX_CAMERA_FOCUS_DISTANCE = -0.001;
    private static final Vec3 UP_IN_CAMERA = new Vec3(0,-1,0);

    // camera parameters
    private double defaultHeightInPixels = 400.0;
    private double widthInPixels = defaultHeightInPixels;
    private double heightInPixels = defaultHeightInPixels;
    private org.janelia.it.workstation.gui.viewer3d.VolumeModel volumeModel;
    private boolean resetFirstRedraw;
    private boolean hasBeenReset = false;

    private Logger logger;

    // scene objects
    public MipRenderer() {
        logger = LoggerFactory.getLogger(MipRenderer.class);
		// actors.add(new TeapotActor()); // solid shading is not supported right now
        volumeModel = new org.janelia.it.workstation.gui.viewer3d.VolumeModel();
        BasicObservableCamera3d camera3d = new BasicObservableCamera3d();
        camera3d.setFocus( 0.0, 0.0, -DEFAULT_CAMERA_FOCUS_DISTANCE );
        getVolumeModel().setCamera3d(camera3d);
    }
    
    public void centerOnPixel(Point p) {
    		// System.out.println("center");
    		double dx =  p.x - widthInPixels/2.0;
    		double dy = heightInPixels/2.0 - p.y;
    		translatePixels(-dx, dy, 0.0);
    }
    
    public void clear() {
		actors.clear();
        hasBeenReset = false;
    }

    public void requestReset() {

    }

    @Override
    public void display(GLAutoDrawable glDrawable) 
    {
	    super.display(glDrawable); // fills background
        widthInPixels = glDrawable.getWidth();
        heightInPixels = glDrawable.getHeight();
        if (resetFirstRedraw && (! hasBeenReset)) {
            resetView();
            hasBeenReset = true;
        }

        //final GL2 gl = glDrawable.getGL().getGL2();
        final GL2Adapter gl = GL2AdapterFactory.createGL2Adapter( glDrawable );
        gl.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
        gl.glPushMatrix();
        updateProjection(gl);
        gl.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        glDrawable.getWidth();
        Vec3 f = volumeModel.getCamera3d().getFocus();    // This is what allows (follows) drag in X and Y.
        Rotation3d rotation = getVolumeModel().getCamera3d().getRotation();
        Vec3 u = rotation.times( UP_IN_CAMERA );
        double unitsPerPixel = glUnitsPerPixel();
        Vec3 c = f.plus(rotation.times(volumeModel.getCameraDepth().times(unitsPerPixel)));
        gl.gluLookAt(c.x(), c.y(), c.z(), // camera in ground
                f.x(), f.y(), f.z(), // focus in ground
                u.x(), u.y(), u.z()); // up vector in ground

        if ( System.getProperty( "glComposablePipelineDebug", "f" ).toLowerCase().startsWith("t") ) {
            DebugGL2 debugGl2 = new org.janelia.it.workstation.gui.viewer3d.error_trap.JaneliaDebugGL2(glDrawable);
            glDrawable.setGL(debugGl2);
        }

        java.util.List<GLActor> localActors = new java.util.ArrayList<GLActor>( actors );
        for (GLActor actor : localActors)
            actor.display(glDrawable);

        gl.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
        gl.glPopMatrix();
    }
 
    public double glUnitsPerPixel() {
        return Math.abs( volumeModel.getCameraFocusDistance() ) / DISTANCE_TO_SCREEN_IN_PIXELS;
    }

    public void resetView()
    {
        // Adjust view to fit the actual objects present
        BoundingBox3d boundingBox = getBoundingBox();
        volumeModel.getCamera3d().setFocus(boundingBox.getCenter());
        getVolumeModel().getCamera3d().resetRotation();
        resetCameraDepth(boundingBox);
    }

    @Override
    public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height)
    {
        this.widthInPixels = width;
        this.heightInPixels = height;

        // System.out.println("reshape() called: x = "+x+", y = "+y+", width = "+width+", height = "+height);
        //final GL2 gl = glDrawable.getGL().getGL2();
        GL2Adapter gl2Adapter = GL2AdapterFactory.createGL2Adapter( glDrawable );
 
        updateProjection(gl2Adapter);
        gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
        gl2Adapter.glLoadIdentity();

        double previousFocusDistance = volumeModel.getCameraFocusDistance();
        if ( previousFocusDistance == DEFAULT_CAMERA_FOCUS_DISTANCE ) {
            BoundingBox3d boundingBox = getBoundingBox();
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
				widthInPixels*widthInPixels 
				+ heightInPixels*heightInPixels);
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
		volumeModel.getCamera3d().getFocus().plusEquals(
                getVolumeModel().getCamera3d().getRotation().times(t)
        );
	}
	
	public void updateProjection(GL2Adapter gl) {
        gl.getGL2GL3().glViewport(0, 0, (int) widthInPixels, (int) heightInPixels);
        double verticalApertureInDegrees = 180.0/Math.PI * 2.0 * Math.abs(
        		Math.atan2(heightInPixels/2.0, DISTANCE_TO_SCREEN_IN_PIXELS));
        gl.glMatrixMode( GL2Adapter.MatrixMode.GL_PROJECTION );
        gl.glLoadIdentity();
        final float h = (float) widthInPixels / (float) heightInPixels;
        double cameraFocusDistance = volumeModel.getCameraFocusDistance();
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

        double cameraFocusDistance = volumeModel.getCameraFocusDistance();
        cameraFocusDistance /= zoomRatio;
        if ( cameraFocusDistance > MAX_CAMERA_FOCUS_DISTANCE ) {
            return;
        }
        if ( cameraFocusDistance < MIN_CAMERA_FOCUS_DISTANCE ) {
            return;
        }
        getVolumeModel().setCameraPixelsPerSceneUnit( DISTANCE_TO_SCREEN_IN_PIXELS, cameraFocusDistance );

        volumeModel.setCameraDepth(new Vec3(0.0, 0.0, cameraFocusDistance));

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

    public org.janelia.it.workstation.gui.viewer3d.VolumeModel getVolumeModel() {
        return volumeModel;
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

    private void resetCameraDepth(BoundingBox3d boundingBox) {
        double heightInMicrometers = boundingBox.getHeight();
        if (heightInMicrometers <= 0.0) { // watch for NaN!
            logger.warn("Adjusted height to account for zero-height bounding box.");
            heightInMicrometers = 2.0; // whatever
        }
        // System.out.println("Focus = " + focusInGround);
        // cameraFocusDistance = DEFAULT_CAMERA_FOCUS_DISTANCE * defaultHeightInPixels / heightInPixels;
        double finalAspectRatio = maxAspectRatio(boundingBox);
        double newFocusDistance = finalAspectRatio * 1.05 * DISTANCE_TO_SCREEN_IN_PIXELS * heightInMicrometers / heightInPixels;
        volumeModel.setCameraDepth( new Vec3( 0.0, 0.0, -newFocusDistance ) );
        getVolumeModel().setCameraPixelsPerSceneUnit(DISTANCE_TO_SCREEN_IN_PIXELS, getVolumeModel().getCameraFocusDistance());
    }

    private BoundingBox3d getBoundingBox() {
        BoundingBox3d boundingBox = new BoundingBox3d();
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
        org.janelia.it.workstation.gui.viewer3d.VolumeModel model = getVolumeModel();
        return (double) Math.min(
            model.getVoxelMicrometers()[0],
            Math.min(
                model.getVoxelMicrometers()[1],
                model.getVoxelMicrometers()[2]
            )
        );
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