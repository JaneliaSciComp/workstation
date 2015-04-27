/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import javax.media.opengl.GL2GL3;
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.Vec3;
import static org.janelia.it.workstation.gui.viewer3d.BaseRenderer.DISTANCE_TO_SCREEN_IN_PIXELS;
import org.janelia.it.workstation.gui.viewer3d.MeshViewContext;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.ViewMatrixSupport;

/**
 * Delegates management of Perspective and Model/View matrices, to keep
 * those details out of the actor.
 *
 * @author fosterl
 */
public class MatrixManager {
    private static final Vec3 UP_IN_CAMERA = new Vec3(0, -1, 0);
    private static final double ASSUMED_FOCUS_DISTANCE = -DISTANCE_TO_SCREEN_IN_PIXELS * 1.5;
    private static final Vec3 FOCUS = new Vec3(0,0,0);
    private static final Vec3 ASSUMED_CAMERA_DEPTH = new Vec3(0, 0, ASSUMED_FOCUS_DISTANCE);
    
    private final MeshViewContext context;
    private int widthInPixels;
    private int heightInPixels;

    public MatrixManager(MeshViewContext context, int widthInPixels, int heightInPixels) {
        this.context = context;
        this.widthInPixels = widthInPixels;
        this.heightInPixels = heightInPixels;
    }
    
    /**
     * Matrices are recalculated from this 'signal'.
     */
    public void recalculate(GL2GL3 gl) {
        updateModelView(gl);
        updateProjection(gl);
    }
    
    private void updateModelView(GL2GL3 gl) {
        // Update Model/View matrix.
        Vec3 f = FOCUS;  // Disallows (does not follow) drag in X and Y.
        Rotation3d rotation = context.getCamera3d().getRotation();
        Vec3 u = rotation.times(UP_IN_CAMERA);
        double unitsPerPixel = glUnitsPerPixel();
        Vec3 c = f.plus(rotation.times(ASSUMED_CAMERA_DEPTH.times(unitsPerPixel)));
        float[] viewingTransform =
                new ViewMatrixSupport().getLookAt(c, f, u);
        context.setModelViewMatrix(viewingTransform);

    }
    
    private double glUnitsPerPixel() {
        return Math.abs(ASSUMED_FOCUS_DISTANCE) / DISTANCE_TO_SCREEN_IN_PIXELS;
    }

    private void updateProjection(GL2GL3 gl) {
        gl.glViewport(0, 0, (int) getWidthInPixels(), (int) getHeightInPixels());
        double verticalApertureInDegrees = 180.0 / Math.PI * 2.0 * Math.abs(
                Math.atan2(getHeightInPixels() / 2.0, DISTANCE_TO_SCREEN_IN_PIXELS));
        final float h = (float) getWidthInPixels() / (float) getHeightInPixels();
        double cameraFocusDistance = ASSUMED_FOCUS_DISTANCE;
        double scaledFocusDistance = Math.abs(cameraFocusDistance) * glUnitsPerPixel();

        ViewMatrixSupport viewMatrixSupport = new ViewMatrixSupport();
        float[] perspective = viewMatrixSupport.getPerspectiveMatrix(
                verticalApertureInDegrees, h,
                0.5 * scaledFocusDistance, 2.0 * scaledFocusDistance
        );

        context.setPerspectiveMatrix(perspective);

    }
    
    /** Picking likely values for these. Avoids caching from renderer */
    private int getWidthInPixels() {
        return widthInPixels;
    }
    
    private int getHeightInPixels() {
        return heightInPixels;
    }
}
