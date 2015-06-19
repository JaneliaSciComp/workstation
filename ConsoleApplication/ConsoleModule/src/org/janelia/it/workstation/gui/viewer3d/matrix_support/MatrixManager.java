/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.matrix_support;

import javax.media.opengl.GL2GL3;
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.Vec3;
import static org.janelia.it.workstation.gui.viewer3d.BaseRenderer.DISTANCE_TO_SCREEN_IN_PIXELS;
import org.janelia.it.workstation.gui.viewer3d.MeshViewContext;

/**
 * Delegates management of Perspective and Model/View matrices, to keep
 * those details out of the actor.
 *
 * @author fosterl
 */
public class MatrixManager {
    
    public enum FocusBehavior {
        FIXED,    // Fixed behavior disallows (does not follow) drag in X and Y.
        DYNAMIC 
    }
    
    private static final Vec3 UP_IN_CAMERA = new Vec3(0, -1, 0);
    private static final double ASSUMED_FOCUS_DISTANCE = -DISTANCE_TO_SCREEN_IN_PIXELS * 1.5;
    private static final Vec3 FOCUS = new Vec3(0,0,0);
    private static final Vec3 ASSUMED_CAMERA_DEPTH = new Vec3(0, 0, ASSUMED_FOCUS_DISTANCE);
    
    private Vec3 focus = FOCUS.clone();
    private final MeshViewContext context;
    private WindowDef windowDef;
    
    private FocusBehavior focusBehavior = FocusBehavior.DYNAMIC;

    public MatrixManager(MeshViewContext context, int widthInPixels, int heightInPixels) {
        this(context, widthInPixels, heightInPixels, FocusBehavior.DYNAMIC);
    }

    public MatrixManager(MeshViewContext context, final int widthInPixels, final int heightInPixels, FocusBehavior focusBehavior) {
        this(
            context,
            new WindowDef() {
                @Override
                public int getWidthInPixels() {
                    return widthInPixels;
                }
                @Override
                public int getHeightInPixels() {
                    return heightInPixels;
                }
            }, 
            focusBehavior
        );
    }
    
    public MatrixManager(MeshViewContext context, WindowDef windowDef, FocusBehavior focusBehavior) {
        this.context = context;
        this.focusBehavior = focusBehavior;
        this.windowDef = windowDef;
        if (focusBehavior == FocusBehavior.FIXED) {
            focus.setX(0);
            focus.setY(windowDef.getWidthInPixels() / 2.0);
            focus.setZ(windowDef.getHeightInPixels() / 2.0);
        }
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
        Vec3 f = focusBehavior == FocusBehavior.FIXED ? focus : context.getCamera3d().getFocus();
        Rotation3d rotation = context.getCamera3d().getRotation();
        Vec3 u = rotation.times(UP_IN_CAMERA);
        double unitsPerPixel = glUnitsPerPixel();
        Vec3 c = f.plus(rotation.times(getCameraDepth().times(unitsPerPixel)));
        float[] viewingTransform =
                new ViewMatrixSupport().getLookAt(c, f, u);
        context.setModelViewMatrix(viewingTransform);

    }
    
    private double glUnitsPerPixel() {
        return Math.abs(getFocusDistance()) / DISTANCE_TO_SCREEN_IN_PIXELS;
    }

    private void updateProjection(GL2GL3 gl) {
        gl.glViewport(0, 0, (int) getWidthInPixels(), (int) getHeightInPixels());
        double verticalApertureInDegrees = 180.0 / Math.PI * 2.0 * Math.abs(
                Math.atan2(getHeightInPixels() / 2.0, DISTANCE_TO_SCREEN_IN_PIXELS));
        final float h = (float) getWidthInPixels() / (float) getHeightInPixels();
        double cameraFocusDistance = getFocusDistance();
        double scaledFocusDistance = Math.abs(cameraFocusDistance) * glUnitsPerPixel();

        ViewMatrixSupport viewMatrixSupport = new ViewMatrixSupport();
        float[] perspective = viewMatrixSupport.getPerspectiveMatrix(
                verticalApertureInDegrees, h,
                10.0f, 2.0 * scaledFocusDistance
        );

        context.setPerspectiveMatrix(perspective);

    }
    
    /** Picking likely values for these. Avoids caching from renderer */
    private int getWidthInPixels() {
        return windowDef.getWidthInPixels();
    }
    
    private int getHeightInPixels() {
        return windowDef.getHeightInPixels();
    }
    
    private double getFocusDistance() {
        if (focusBehavior == FocusBehavior.FIXED) {
            return ASSUMED_FOCUS_DISTANCE;
        }
        else if (focusBehavior == FocusBehavior.DYNAMIC) {
            return context.getCameraFocusDistance();
        }
        else {
            return 0.0;
        }
    }
    
    private Vec3 getCameraDepth() {
        if (focusBehavior == FocusBehavior.FIXED) {
            return ASSUMED_CAMERA_DEPTH;
        } else if (focusBehavior == FocusBehavior.DYNAMIC) {
            return context.getCameraDepth();
        } else {
            return context.getCameraDepth();
        }
    }
    
    public static interface WindowDef {
        int getWidthInPixels();
        int getHeightInPixels();
    }
}
