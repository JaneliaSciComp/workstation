/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.workstation.mesh_loader;
/**
 * This establishes the only possible normals that can be produced when
 * enveloping a cubic voxel that is oriented with all its faces parallel to
 * axes.
 */
public enum AxialNormalDirection {

    TOP_FACE_NORMAL, FRONT_FACE_NORMAL, BOTTOM_FACE_NORMAL, LEFT_FACE_NORMAL, RIGHT_FACE_NORMAL, BACK_FACE_NORMAL, NOT_APPLICABLE;

    private static final float[] TOP_FACE_NORMAL_VECT = new float[]{0, 1, 0};
    private static final float[] BOTTOM_FACE_NORMAL_VECT = new float[]{0, -1, 0};
    private static final float[] FRONT_FACE_NORMAL_VECT = new float[]{0, 0, 1};
    private static final float[] RIGHT_FACE_NORMAL_VECT = new float[]{1, 0, 0};
    private static final float[] LEFT_FACE_NORMAL_VECT = new float[]{-1, 0, 0};
    private static final float[] BACK_FACE_NORMAL_VECT = new float[]{0, 0, -1};
    
    public float[] getNumericElements() {
        float[] rtnVal;
        switch (this) {
            case TOP_FACE_NORMAL:
                rtnVal = TOP_FACE_NORMAL_VECT;
                break;
            case FRONT_FACE_NORMAL:
                rtnVal = FRONT_FACE_NORMAL_VECT;
                break;
            case BOTTOM_FACE_NORMAL:
                rtnVal = BOTTOM_FACE_NORMAL_VECT;
                break;
            case LEFT_FACE_NORMAL:
                rtnVal = LEFT_FACE_NORMAL_VECT;
                break;
            case RIGHT_FACE_NORMAL:
                rtnVal = RIGHT_FACE_NORMAL_VECT;
                break;
            case BACK_FACE_NORMAL:
                rtnVal = BACK_FACE_NORMAL_VECT;
                break;
            default:
                // This will occur if not applicable.
                rtnVal = null;
        }
        return rtnVal;
    }
}

