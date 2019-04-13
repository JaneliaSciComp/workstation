package org.janelia.horta.camera;

import org.janelia.horta.camera.InterpolatorKernel;
import org.janelia.geometry3d.Quaternion;

/**
 *
 * @author brunsc
 */
public class LinearInterpolatorKernel implements InterpolatorKernel
{

    @Override
    public double interpolate_equidistant(double t, double p0, double p1, double p2, double p3) {
        return p1 * (1-t) + p2 * t;
    }

    @Override
    public Quaternion interpolate_equidistant(double t, Quaternion p0, Quaternion p1, Quaternion p2, Quaternion p3) {
        return p1.slerp(p2, (float)t);
    }
    
}
