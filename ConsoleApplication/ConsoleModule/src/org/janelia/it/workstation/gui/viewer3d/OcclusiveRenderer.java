package org.janelia.it.workstation.gui.viewer3d;

import java.awt.Color;
import java.util.ArrayList;
import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.opengl.GL2Adapter;
import org.janelia.it.workstation.gui.opengl.GL2AdapterFactory;
import org.janelia.it.workstation.gui.opengl.GLActor;
import static org.janelia.it.workstation.gui.viewer3d.ActorRenderer.UP_IN_CAMERA;
import org.janelia.it.workstation.gui.viewer3d.error_trap.JaneliaDebugGL2;
import org.janelia.it.workstation.gui.viewer3d.picking.RenderedIdPicker;
import org.slf4j.LoggerFactory;

public class OcclusiveRenderer 
    extends ActorRenderer
{
	// Unknown utility.
	static {
		try {
			GLProfile profile = GLProfile.get(GLProfile.GL3);
			final GLCapabilities capabilities = new GLCapabilities(profile);
			capabilities.setGLProfile(profile);
		} catch (Throwable th) {
			LoggerFactory.getLogger(RenderedIdPicker.class).error("No GL3 profile available");
		}

	}

    private ResetPositionerI resetPositioner;
    
    // scene objects
    public OcclusiveRenderer() {
        super(new OcclusiveVolumeModel());
    }
    
    public void setResetPositioner( ResetPositionerI resetPositioner ) {
        this.resetPositioner = resetPositioner;
    }
    
    @Override
    public void display(GLAutoDrawable glDrawable) {        
        // Preset background from the volume model.
        float[] backgroundClrArr = getVolumeModel().getBackgroundColorFArr();
        this.backgroundColor = new Color( backgroundClrArr[ 0 ], backgroundClrArr[ 1 ], backgroundClrArr[ 2 ] );
	    super.display(glDrawable); // fills background
        
        setWidthInPixels(glDrawable.getWidth());
        setHeightInPixels(glDrawable.getHeight());
        resetOnFirstRedraw();

        //final GL2 gl = glDrawable.getGL().getGL2();
        final GL2Adapter gl = GL2AdapterFactory.createGL2Adapter( glDrawable );

        // TEMP: this should be flagged on or off.
        gl.getGL2GL3().glDisable(GL2.GL_FRAMEBUFFER_SRGB);

        gl.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
        gl.glPushMatrix();
        updateProjection(gl);
        gl.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        glDrawable.getWidth();
        Vec3 f = getVolumeModel().getCamera3d().getFocus();    // This is what allows (follows) drag in X and Y.
        Rotation3d rotation = getVolumeModel().getCamera3d().getRotation();
        Vec3 u = rotation.times( UP_IN_CAMERA );
        double unitsPerPixel = glUnitsPerPixel();
        Vec3 c = f.plus(rotation.times(getVolumeModel().getCameraDepth().times(unitsPerPixel)));
        gl.gluLookAt(c.x(), c.y(), c.z(), // camera in ground
                f.x(), f.y(), f.z(), // focus in ground
                u.x(), u.y(), u.z()); // up vector in ground

        if ( System.getProperty( "glComposablePipelineDebug", "f" ).toLowerCase().startsWith("t") ) {
            DebugGL2 debugGl2 = new JaneliaDebugGL2(glDrawable);
            glDrawable.setGL(debugGl2);
        }

        // Copy member list of actors local for independent iteration.
        for (GLActor actor : new ArrayList<>( actors ))
            actor.display(glDrawable);

        // TEMP: this should be flagged on or off.
        gl.getGL2GL3().glEnable(GL2.GL_FRAMEBUFFER_SRGB);

        gl.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
        gl.glPopMatrix();
    }
    
    @Override
    public void updateProjection(GL2Adapter gl) {
        gl.getGL2GL3().glViewport(0, 0, (int) getWidthInPixels(), (int) getHeightInPixels());
        double verticalApertureInDegrees = 180.0 / Math.PI * 2.0 * Math.abs(
                Math.atan2(getHeightInPixels() / 2.0, DISTANCE_TO_SCREEN_IN_PIXELS));
        gl.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
        gl.glLoadIdentity();
        final float h = (float) getWidthInPixels() / (float) getHeightInPixels();
        double cameraFocusDistance = getVolumeModel().getCameraFocusDistance();
        double scaledFocusDistance = Math.abs(cameraFocusDistance) * glUnitsPerPixel();
        glu.gluPerspective(verticalApertureInDegrees,
                h,
                0.005 * scaledFocusDistance,
                5.0 * scaledFocusDistance);
    }

    @Override
    public void resetView() {
        super.resetView();
        if (resetPositioner != null) {
            resetPositioner.resetView();
        }
    }
    
    /** Making this accessible for external use. */
    @Override
    public void resetCameraDepth(BoundingBox3d boundingBox) {
        super.resetCameraDepth(boundingBox);
    }

    @Override
    protected void displayBackground(GL2 gl) 
    {
        // paint solid background color
	    gl.glClearColor(
	    		backgroundColor.getRed()/255.0f,
	    		backgroundColor.getGreen()/255.0f,
	    		backgroundColor.getBlue()/255.0f,
	    		backgroundColor.getAlpha()/255.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);    		
    }
    
    public static class OcclusiveVolumeModel extends VolumeModel {
        /**
         * Never return true for white-background.  Want colors remaining
         * same,
         * @return always false;
         */
        @Override
        public boolean isWhiteBackground() {
            return false;
        }
    }
}