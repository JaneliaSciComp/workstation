/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.publication_quality.mesh.actor;

import java.awt.Color;
import java.util.ArrayList;
import javax.media.opengl.DebugGL2;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.opengl.GL2Adapter;
import org.janelia.it.workstation.gui.opengl.GL2AdapterFactory;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.ActorRenderer;
import org.janelia.it.workstation.gui.viewer3d.MeshViewContext;
import org.janelia.it.workstation.gui.viewer3d.error_trap.JaneliaDebugGL2;
import org.janelia.it.workstation.publication_quality.mesh.MeshViewer;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.ViewMatrixSupport;

/**
 * Renders mesh-based 3D objects.
 * @author fosterl
 */
public class MeshRenderer extends ActorRenderer {
    @Override
    public void updateProjection(GL2Adapter gl) {
        gl.getGL2GL3().glViewport(0, 0, (int) getWidthInPixels(), (int) getHeightInPixels());
        double verticalApertureInDegrees = 180.0 / Math.PI * 2.0 * Math.abs(
                Math.atan2(getHeightInPixels() / 2.0, DISTANCE_TO_SCREEN_IN_PIXELS));
        final float h = (float) getWidthInPixels() / (float) getHeightInPixels();
        double cameraFocusDistance = getVolumeModel().getCameraFocusDistance();
        double scaledFocusDistance = Math.abs(cameraFocusDistance) * glUnitsPerPixel();

        ViewMatrixSupport viewMatrixSupport = new ViewMatrixSupport();
        float[] perspective = viewMatrixSupport.getPerspectiveMatrix(
                verticalApertureInDegrees, h,
                0.5 * scaledFocusDistance, 2.0 * scaledFocusDistance
        );

        ((MeshViewContext)getVolumeModel()).setPerspectiveMatrix(perspective);

    }
    @Override
    public void display(GLAutoDrawable glDrawable) {

        Vec3 f = getVolumeModel().getCamera3d().getFocus();    // This is what allows (follows) drag in X and Y.
        Rotation3d rotation = getVolumeModel().getCamera3d().getRotation();
        Vec3 u = rotation.times( UP_IN_CAMERA );
        double unitsPerPixel = glUnitsPerPixel();
        Vec3 c = f.plus(rotation.times(getVolumeModel().getCameraDepth().times(unitsPerPixel)));
        float[] viewingTransform = //new ViewMatrixSupport().getIdentityMatrix();
                new ViewMatrixSupport().getLookAt(c, f, u);
        ((MeshViewContext)getVolumeModel()).setModelViewMatrix( viewingTransform );
        
        // Preset background from the volume model.
        float[] backgroundClrArr = getVolumeModel().getBackgroundColorFArr();
        this.backgroundColor = new Color( backgroundClrArr[ 0 ], backgroundClrArr[ 1 ], backgroundClrArr[ 2 ] );
        
        setWidthInPixels(glDrawable.getWidth());
        setHeightInPixels(glDrawable.getHeight());
        resetOnFirstRedraw();

        final GL2Adapter gl = GL2AdapterFactory.createGL2Adapter( glDrawable );
        updateProjection(gl);

        if ( System.getProperty( "glComposablePipelineDebug", "f" ).toLowerCase().startsWith("t") ) {
            DebugGL2 debugGl2 = new JaneliaDebugGL2(glDrawable);
            glDrawable.setGL(debugGl2);
        }

        // Copy member list of actors local for independent iteration.
	    super.display(glDrawable); // fills background
        for (GLActor actor : new ArrayList<>( actors ))
            actor.display(glDrawable);
    }

}
