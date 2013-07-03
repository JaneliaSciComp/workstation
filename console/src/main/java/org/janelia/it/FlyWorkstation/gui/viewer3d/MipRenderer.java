package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.error_trap.JaneliaDebugGL2;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import java.awt.*;
 
class MipRenderer 
    extends BaseRenderer
{
    private static final double DEFAULT_CAMERA_FOCUS_DISTANCE = 20.0;

    // camera parameters
    private Vec3 upInCamera = new Vec3(0,-1,0);
    private double distanceToScreenInPixels = 2000;
    private double defaultHeightInPixels = 400.0;
    private double widthInPixels = defaultHeightInPixels;
    private double heightInPixels = defaultHeightInPixels;
    private VolumeModel volumeModel;

    // scene objects
    public MipRenderer() {
		// actors.add(new TeapotActor()); // solid shading is not supported right now
        volumeModel = new VolumeModel();
        BasicCamera3d camera3d = new BasicCamera3d();
        camera3d.setFocus( 0.0, 0.0, -DEFAULT_CAMERA_FOCUS_DISTANCE );
        getVolumeModel().setCamera3d(camera3d);
        addActor( new VolumeBrick( getVolumeModel() ) );
    }
    
    public void centerOnPixel(Point p) {
    		// System.out.println("center");
    		double dx =  p.x - widthInPixels/2.0;
    		double dy = heightInPixels/2.0 - p.y;
    		translatePixels(-dx, dy, 0.0);
    }
    
    public void clear() {
		actors.clear();
    }

    @Override
    public void display(GLAutoDrawable gLDrawable) 
    {
	    super.display(gLDrawable); // fills background
        widthInPixels = gLDrawable.getWidth();
        heightInPixels = gLDrawable.getHeight();

        final GL2 gl = gLDrawable.getGL().getGL2();
        gl.glPushAttrib(GL2.GL_TRANSFORM_BIT);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        updateProjection(gl);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gLDrawable.getWidth();
        Vec3 f = volumeModel.getFocusInGround();
        Rotation3d rotation = getVolumeModel().getCamera3d().getRotation();
        Vec3 u = rotation.times(upInCamera);
        Vec3 c = f.plus(rotation.times( volumeModel.getCamera3d().getFocus()) );
        glu.gluLookAt(c.x(), c.y(), c.z(), // camera in ground
                f.x(), f.y(), f.z(), // focus in ground
                u.x(), u.y(), u.z()); // up vector in ground

        if ( System.getProperty( "glComposablePipelineDebug", "f" ).toLowerCase().startsWith("t") ) {
            DebugGL2 debugGl2 = new JaneliaDebugGL2(gl);
            gLDrawable.setGL(debugGl2);
        }

        java.util.List<GLActor> localActors = new java.util.ArrayList<GLActor>( actors );
        for (GLActor actor : localActors)
            actor.display(gl);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glPopAttrib();
        gl.glFlush();
    }
 
    public double glUnitsPerPixel() {
    		return Math.abs( volumeModel.getCamera3d().getFocus().getZ() ) / distanceToScreenInPixels;
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
    public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height)
    {
        this.widthInPixels = width;
        this.heightInPixels = height;

        // System.out.println("reshape() called: x = "+x+", y = "+y+", width = "+width+", height = "+height);
        final GL2 gl = gLDrawable.getGL().getGL2();
 
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
        		Math.atan2(heightInPixels/2.0, distanceToScreenInPixels));
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
		if (zoomRatio <= 0.0)
			return;
		if (zoomRatio == 1.0)
			return;
        double cameraFocusDistance = volumeModel.getCamera3d().getFocus().getZ();
		cameraFocusDistance /= zoomRatio;
        volumeModel.getCamera3d().setFocus( 0.0, 0.0, cameraFocusDistance );
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
        double cameraFocusDistance = finalAspectRatio * 1.05 * distanceToScreenInPixels * heightInMicrometers / heightInPixels;
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

}