package org.janelia.horta.camera;

import org.janelia.geometry3d.Quaternion;

/**
 * Every interpolatable I am aware of can be reduced to either double or
 * Quaternion interpolation
 * @author brunsc
 */
public interface InterpolatorKernel 
{
    // General case does not require points to be spaced equally
    double interpolate_equidistant(double t, // t in range [0-1], between points p1 and p2
            double p0, double p1, double p2, double p3); // values at 4 points

    // General case does not require points to be spaced equally
    Quaternion interpolate_equidistant(double t, // t in range [0-1], between points p1 and p2
            Quaternion p0, Quaternion p1, Quaternion p2, Quaternion p3); // values at 4 points
}
