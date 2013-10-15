package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.geom.Rotation3d;
import org.janelia.it.FlyWorkstation.geom.UnitVec3;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.error_trap.JaneliaDebugGL2;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import java.awt.*;
 
class MipRenderer 
    extends BaseRenderer
{
    public static final double DISTANCE_TO_SCREEN_IN_PIXELS = 2000;

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
    private VolumeModel volumeModel;
    private boolean resetFirstRedraw;
    private boolean hasBeenReset = false;

    // scene objects
    public MipRenderer() {
		// actors.add(new TeapotActor()); // solid shading is not supported right now
        volumeModel = new VolumeModel();
        BasicObservableCamera3d camera3d = new BasicObservableCamera3d();
        camera3d.setFocus( 0.0, 0.0, -DEFAULT_CAMERA_FOCUS_DISTANCE );
        getVolumeModel().setCamera3d(camera3d);
//        addActor( new MultiTexVolumeBrick( getVolumeModel() ) );
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

        final GL2 gl = glDrawable.getGL().getGL2();
        gl.glPushAttrib(GL2.GL_TRANSFORM_BIT);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        updateProjection(gl);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        glDrawable.getWidth();
        Vec3 f = volumeModel.getFocusInGround();    // This is what allows (follows) drag in X and Y.
        Rotation3d rotation = getVolumeModel().getCamera3d().getRotation();
        Vec3 u = rotation.times( UP_IN_CAMERA );
        Vec3 c = f.plus(rotation.times( volumeModel.getCamera3d().getFocus()) );
        glu.gluLookAt(c.x(), c.y(), c.z(), // camera in ground
                f.x(), f.y(), f.z(), // focus in ground
                u.x(), u.y(), u.z()); // up vector in ground

        if ( System.getProperty( "glComposablePipelineDebug", "f" ).toLowerCase().startsWith("t") ) {
            DebugGL2 debugGl2 = new JaneliaDebugGL2(gl);
            glDrawable.setGL(debugGl2);
        }

        java.util.List<GLActor> localActors = new java.util.ArrayList<GLActor>( actors );
        for (GLActor actor : localActors)
            actor.display(glDrawable);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glPopAttrib();
        gl.glFlush();
    }
 
    public double glUnitsPerPixel() {
        return Math.abs( volumeModel.getCamera3d().getFocus().getZ() ) / DISTANCE_TO_SCREEN_IN_PIXELS;
    }

    public double getVoxelsPerSceneUnit() {
        return Math.abs( DISTANCE_TO_SCREEN_IN_PIXELS / volumeModel.getCamera3d().getFocus().getZ() );
    }

    public void resetView()
    {
        // Adjust view to fit the actual objects present
        BoundingBox3d boundingBox = getBoundingBox();
        volumeModel.setFocusInGround( boundingBox.getCenter() );
        getVolumeModel().getCamera3d().resetRotation();
        resetCameraFocus(boundingBox);
    }

    @Override
    public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height)
    {
        this.widthInPixels = width;
        this.heightInPixels = height;

        // System.out.println("reshape() called: x = "+x+", y = "+y+", width = "+width+", height = "+height);
        final GL2 gl = glDrawable.getGL().getGL2();
 
        updateProjection(gl);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        double previousFocusDistance = volumeModel.getCamera3d().getFocus().getZ();
        if ( previousFocusDistance == DEFAULT_CAMERA_FOCUS_DISTANCE ) {
            BoundingBox3d boundingBox = getBoundingBox();
            resetCameraFocus( boundingBox );
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
		Vec3 t = new Vec3(-dx, -dy, -dz).times(glUnitsPerPixel());
		volumeModel.getFocusInGround().plusEquals(getVolumeModel().getCamera3d().getRotation().times(t));
	}
	
	public void updateProjection(GL2 gl) {
        gl.glViewport(0, 0, (int)widthInPixels, (int)heightInPixels);
        double verticalApertureInDegrees = 180.0/Math.PI * 2.0 * Math.abs(
        		Math.atan2(heightInPixels/2.0, DISTANCE_TO_SCREEN_IN_PIXELS));
        gl.glPushAttrib(GL2.GL_TRANSFORM_BIT);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        final float h = (float) widthInPixels / (float) heightInPixels;
        double cameraFocusDistance = Math.abs(volumeModel.getCamera3d().getFocus().getZ());
        glu.gluPerspective(verticalApertureInDegrees,
        		h, 
        		0.5 * cameraFocusDistance, 
        		2.0 * cameraFocusDistance);
        gl.glPopAttrib();
	}
	
	public void zoom(double zoomRatio) {
		if (zoomRatio <= 0.0) {
			return;
        }
		if (zoomRatio == 1.0) {
			return;
        }

        double cameraFocusDistance = volumeModel.getCamera3d().getFocus().getZ();
        cameraFocusDistance /= zoomRatio;
        if ( cameraFocusDistance > MAX_CAMERA_FOCUS_DISTANCE ) {
            return;
        }
        if ( cameraFocusDistance < MIN_CAMERA_FOCUS_DISTANCE ) {
            return;
        }
        getVolumeModel().getCamera3d().setPixelsPerSceneUnit( Math.abs( DISTANCE_TO_SCREEN_IN_PIXELS / cameraFocusDistance ) );

        volumeModel.getCamera3d().setFocus(0.0, 0.0, cameraFocusDistance);


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

    public VolumeModel getVolumeModel() {
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

    private void resetCameraFocus(BoundingBox3d boundingBox) {
        double heightInMicrometers = boundingBox.getHeight();
        if (! (heightInMicrometers >= 0.0)) // watch for NaN!
            heightInMicrometers = 2.0; // whatever

        // System.out.println("Focus = " + focusInGround);
        // cameraFocusDistance = DEFAULT_CAMERA_FOCUS_DISTANCE * defaultHeightInPixels / heightInPixels;
        double finalAspectRatio = maxAspectRatio(boundingBox);
        double cameraFocusDistance = finalAspectRatio * 1.05 * DISTANCE_TO_SCREEN_IN_PIXELS * heightInMicrometers / heightInPixels;
        volumeModel.getCamera3d().setFocus( 0.0, 0.0, -cameraFocusDistance );

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
        VolumeModel model = getVolumeModel();
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
////            Vec3 rotSize = getVolumeModel().getFocusInGround().transpose().times(volSize);
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