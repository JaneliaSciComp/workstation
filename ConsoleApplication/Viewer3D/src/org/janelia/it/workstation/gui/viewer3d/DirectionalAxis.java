/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d;

import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Rotation3d;
import org.janelia.it.jacs.shared.geom.Vec3;

/**
 * Enum of all possible axes, including direction-facing.
 * 
 * @author fosterl
 */
public enum DirectionalAxis {
    NEGATIVE_X(CoordinateAxis.X, -1.0),
    NEGATIVE_Y(CoordinateAxis.Y, -1.0), 
    NEGATIVE_Z(CoordinateAxis.Z, -1.0), 
    POSITIVE_X(CoordinateAxis.X,  1.0),
    POSITIVE_Y(CoordinateAxis.Y,  1.0), 
    POSITIVE_Z(CoordinateAxis.Z,  1.0);
    
    private CoordinateAxis axis;
    private double direction = 1.0;
    
    public static DirectionalAxis findAxis(Rotation3d rotation) {
		// "InGround" means in the WORLD object reference frame.
        // (the view vector in the EYE reference frame is always [0,0,-1])
        Vec3 viewVectorInGround = rotation.times(new Vec3(0, 0, 1));

        // Compute the principal axis of the view direction; that's the direction we will slice along.
        CoordinateAxis a1 = CoordinateAxis.X; // First guess principal axis is X.  Who knows?
        Vec3 vv = viewVectorInGround;
        if (Math.abs(vv.y()) > Math.abs(vv.get(a1.index()))) {
            a1 = CoordinateAxis.Y; // OK, maybe Y axis is principal
        }
        if (Math.abs(vv.z()) > Math.abs(vv.get(a1.index()))) {
            a1 = CoordinateAxis.Z; // Alright, it's definitely Z principal.
        }

        // If principal axis points away from viewer, draw slices front to back,
        // instead of back to front.
        double direction = 1.0; // points away from viewer, render back to front, n to 0
        if (vv.get(a1.index()) < 0.0) {
            direction = -1.0; // points toward, front to back, 0 to n
        }
        
        DirectionalAxis da = null;
        if (direction > 0.0) {
            switch (a1) {
                case X:
                    da = POSITIVE_X;
                    break;
                case Y:
                    da = POSITIVE_Y;
                    break;
                case Z:
                    da = POSITIVE_Z;
                    break;
            }
        }
        else {
            switch (a1) {
                case X:
                    da = NEGATIVE_X;
                    break;
                case Y:
                    da = NEGATIVE_Y;
                    break;
                case Z:
                    da = NEGATIVE_Z;
                    break;
            }
        }
        
        return da;
    }

    public CoordinateAxis getCoordinateAxis() {
        return axis;
    }

    public double getDirection() {
        return direction;
    }

    private DirectionalAxis(CoordinateAxis axis, double direction) {
        this.direction = direction;
        this.axis = axis;
    }
        
}
