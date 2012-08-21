package org.janelia.it.FlyWorkstation.gui.viewer3d;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import java.awt.Point;
import java.util.Vector;
 
class MipRenderer implements GLEventListener
{
    private GLU glu = new GLU();
    
    // camera parameters
    Vec3 focusInGround = new Vec3(0,0,0);
    private Vec3 upInCamera = new Vec3(0,1,0);
    private Rotation R_ground_camera = new Rotation();
    private double defaultCameraFocusDistance = 20.0;
    double cameraFocusDistance = defaultCameraFocusDistance;
    private double distanceToScreenInPixels = 2000;
    private double defaultHeightInPixels = 400.0;
    private double widthInPixels = defaultHeightInPixels;
    private double heightInPixels = defaultHeightInPixels;
    // scene objects
    private Vector<GLActor> actors = new Vector<GLActor>();


    public MipRenderer() {
    		// actors.add(new TeapotActor());
    		actors.add(new VolumeBrick(this));
    }
    
    public void addActor(GLActor actor) {
    		actors.add(actor);
    }
    
    public void centerOnPixel(Point p) {
    		// System.out.println("center");
    		double dx =  p.x - widthInPixels/2.0;
    		double dy = heightInPixels/2.0 - p.y;
    		translatePixels(dx, dy, 0.0);
    }
    
    public void clear() {
    		actors.clear();
    }
    
    @Override
    public void display(GLAutoDrawable gLDrawable) 
    {
        final GL2 gl = gLDrawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        updateProjection(gl);
        gl.glPushAttrib(GL2.GL_TRANSFORM_BIT);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        		gl.glLoadIdentity();
        		Vec3 f = focusInGround;
        		Vec3 u = R_ground_camera.times(upInCamera);
        		Vec3 c = f.plus(R_ground_camera.times(new Vec3(0,0,cameraFocusDistance)));
        		glu.gluLookAt(c.x(), c.y(), c.z(), // camera in ground
        					  f.x(), f.y(), f.z(), // focus in ground
        					  u.x(), u.y(), u.z()); // up vector in ground
        		for (GLActor actor : actors)
        			actor.display(gl);
        gl.glPopMatrix();
        gl.glPopAttrib();
        gl.glFlush();
    }
 
    public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) 
    {
    		// System.out.println("displayChanged called");
    }
    
    @Override
	public void dispose(GLAutoDrawable glDrawable) 
	{
        final GL2 gl = glDrawable.getGL().getGL2();
		for (GLActor actor : actors)
			actor.dispose(gl);
	}

    public Rotation getRotation() {
    		return R_ground_camera;
    }

    public double glUnitsPerPixel() {
    		return cameraFocusDistance / distanceToScreenInPixels;
    }

    @Override
    public void init(GLAutoDrawable gLDrawable) 
    {
    		// System.out.println("init() called");
        GL2 gl = gLDrawable.getGL().getGL2();
        gl.glEnable(GL2.GL_FRAMEBUFFER_SRGB);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        // gl.glClearColor(0.1f, 0.1f, 0.5f, 0.0f);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        // gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        float[] specColor = {1f,1f,1f,1f};
        float[] shininess = {50f};
        gl.glMaterialfv (GL2.GL_FRONT, GL2.GL_SPECULAR, specColor, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SHININESS, shininess, 0);
        float[] light0Pos = {1f,1f,1f,0f};
        float[] light0Diffuse = {1f,1f,1f,1f};
        float[] light0Specular = {1f,1f,1f,1f};
        float[] lightAmbient = {1f,1f,1f,0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, light0Pos, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, light0Diffuse, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, light0Specular, 0);
        gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, lightAmbient, 0);
		for (GLActor actor : actors)
			actor.display(gl);
    }
    
    public void resetView() 
    {
    		// Adjust view to fit the actual objects present
    		BoundingBox boundingBox = new BoundingBox();
    		for (GLActor actor : actors) {
    			boundingBox.include(actor.getBoundingBox());
    		}
    		if (boundingBox.isEmpty())
    			boundingBox.include(new Vec3(0,0,0));
    		focusInGround = boundingBox.getCenter();
    		R_ground_camera = new Rotation();
    		double heightInMicrometers = boundingBox.getHeight();
    		if (! (heightInMicrometers >= 0.0)) // watch for NaN!
    			heightInMicrometers = 2.0; // whatever
    		// System.out.println("Focus = " + focusInGround);
    		// System.out.println("Image height = " + heightInMicrometers);
    		cameraFocusDistance = 1.05 * distanceToScreenInPixels * heightInMicrometers / heightInPixels;
    		// cameraFocusDistance = defaultCameraFocusDistance * defaultHeightInPixels / heightInPixels;
    }
    
    @Override
    public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) 
    {
    		// Keep roughly the same view as before by zooming
    		double zoomRatio = 1.0;
    		if (heightInPixels > 0) {
    			zoomRatio = (double)height/heightInPixels;
    		}
    		this.widthInPixels = width;
    		this.heightInPixels = height;
    		// System.out.println("reshape() called: x = "+x+", y = "+y+", width = "+width+", height = "+height);
        final GL2 gl = gLDrawable.getGL().getGL2();
 
        if (height <= 0) // avoid a divide by zero error!
        {
            height = 1;
        }
        if (zoomRatio != 1.0)
        		zoom(zoomRatio);
        updateProjection(gl);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }
    
	public void rotatePixels(double dx, double dy, double dz) {
		// Rotate like a trackball
		double dragDistance = Math.sqrt(dy*dy + dx*dx + dz*dz);
		if (dragDistance <= 0.0)
			return;
		UnitVec3 rotationAxis = new UnitVec3(dy, dx, dz);
		double windowSize = Math.sqrt(
				widthInPixels*widthInPixels 
				+ heightInPixels*heightInPixels);
		// Drag across the entire window to rotate all the way around
		double rotationAngle = 2.0 * Math.PI * dragDistance/windowSize;
		// System.out.println(rotationAxis.toString() + rotationAngle);
		Rotation rotation = new Rotation().setFromAngleAboutUnitVector(
				rotationAngle, rotationAxis);
		// System.out.println(rotation);
		R_ground_camera = R_ground_camera.times(rotation.transpose());
		// System.out.println(R_ground_camera);
	}

	public void translatePixels(double dx, double dy, double dz) {
		// trackball translate
		Vec3 t = new Vec3(dx, dy, dz).times(glUnitsPerPixel());
		focusInGround.plusEquals(R_ground_camera.times(t));
	}
	
	public void updateProjection(GL2 gl) {
        gl.glViewport(0, 0, (int)widthInPixels, (int)heightInPixels);
        double verticalApertureInDegrees = 180.0/Math.PI * 2.0 * Math.abs(
        		Math.atan2(heightInPixels/2.0, distanceToScreenInPixels));
        gl.glPushAttrib(GL2.GL_TRANSFORM_BIT);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        final float h = (float) widthInPixels / (float) heightInPixels;
        glu.gluPerspective(verticalApertureInDegrees,
        		h, 
        		0.3 * cameraFocusDistance, 
        		3.0 * cameraFocusDistance);
        gl.glPopAttrib();
	}
	
	public void zoom(double zoomRatio) {
		if (zoomRatio <= 0.0)
			return;
		if (zoomRatio == 1.0)
			return;
		cameraFocusDistance /= zoomRatio;
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

}