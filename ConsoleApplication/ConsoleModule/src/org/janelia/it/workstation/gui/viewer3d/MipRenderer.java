package org.janelia.it.workstation.gui.viewer3d;

import java.awt.Color;
import java.util.ArrayList;
import javax.media.opengl.DebugGL2;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.opengl.GL2Adapter;
import org.janelia.it.workstation.gui.opengl.GL2AdapterFactory;
import org.janelia.it.workstation.gui.opengl.GLActor;
import static org.janelia.it.workstation.gui.viewer3d.ActorRenderer.UP_IN_CAMERA;
import org.janelia.it.workstation.gui.viewer3d.error_trap.JaneliaDebugGL2;

class MipRenderer 
    extends ActorRenderer
{
    // scene objects
    public MipRenderer() {
        super();
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
        Vec3 cameraDepth = getVolumeModel().getCameraDepth();
        if (cameraDepth == null) {
            cameraDepth = new Vec3(0, 0, f.getZ());
            getVolumeModel().setCameraDepth( cameraDepth ); 
        }
        Vec3 c = f.plus(rotation.times(cameraDepth.times(unitsPerPixel)));
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

        gl.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
        gl.glPopMatrix();
    }

}