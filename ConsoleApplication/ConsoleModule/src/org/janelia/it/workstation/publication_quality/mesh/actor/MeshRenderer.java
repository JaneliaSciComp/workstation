/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.publication_quality.mesh.actor;

import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.opengl.GL2Adapter;
import org.janelia.it.workstation.gui.viewer3d.ActorRenderer;
import org.janelia.it.workstation.publication_quality.mesh.MeshViewer;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.ViewMatrixSupport;
import org.janelia.it.workstation.publication_quality.mesh.MeshViewer.ExtendedVolumeModel;

/**
 *
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

        ((MeshViewer.ExtendedVolumeModel)getVolumeModel()).setPerspectiveMatrix(perspective);

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
        ((ExtendedVolumeModel)getVolumeModel()).setModelViewMatrix( viewingTransform );
        
	    super.display(glDrawable); // fills background
        
    }
 
}
