package org.janelia.horta.camera;

import org.janelia.horta.camera.InterpolatorKernel;
import org.janelia.geometry3d.Quaternion;

/**
 * Spline function for smooth interpolation of viewer states.
 * Catmull-Rom spline has the special property that it passes exactly
 * through all control points, while maintaining continuous first
 * derivatives, resulting in a smooth and graceful fly-through that
 * exactly visits each of the input key frames.
 * 
 * Notice that we include a spherical spline interpolation of Quaternions,
 * which can be used to get even the camera rotation to look very smooth, eliminating
 * that pop-and-lock 1970s robot-dance look that lesser animation tools
 * often exhibit.
 * 
 * @author brunsc
 */
public class CatmullRomSplineKernel implements InterpolatorKernel
{
    
    // Assumes points are spaced equally
    @Override
    public double interpolate_equidistant(double t, double p0, double p1, double p2, double p3)
    {
        return (
              t*((2.0-t)*t - 1.0) * p0
            + (t*t*(3.0*t - 5.0) + 2.0) * p1
            + t*((4.0 - 3.0*t)*t + 1.0) * p2
            + (t-1.0)*t*t * p3 ) / 2.0;
    }

    // specialization for Quaterions
    // Translated from page 449 of "Visualizing Quaternions" by Andrew J. Hanson.
    // TODO: I have no idea how to do a non-uniform version of Quaternion interpolation.
    @Override
    public Quaternion interpolate_equidistant(double t, Quaternion q00, Quaternion q01, Quaternion q02, Quaternion q03)
    {
        Quaternion q10 = q00.slerp(q01, (float)t+1);
        Quaternion q11 = q01.slerp(q02, (float)t);
        Quaternion q12 = q02.slerp(q03, (float)t-1);
        
        Quaternion q20 = q10.slerp(q11, (float)(t+1)/2f);
        Quaternion q21 = q11.slerp(q12, (float)t/2f);
        
        Quaternion result = q20.slerp(q21, (float)t);
        return result;
    }

}
