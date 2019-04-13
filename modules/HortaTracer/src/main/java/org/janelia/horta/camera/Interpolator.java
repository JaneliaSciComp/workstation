package org.janelia.horta.camera;

/**
 *
 * @author brunsc
 */
public interface Interpolator<T> 
{
    // Interpolates value at a particular proportion of the distance between p1 and p2.
    // Assumes points are spaced equally
    T interpolate_equidistant(
            double ofTheWay, // varies between zero and one
            T p0, T p1, T p2, T p3 // neighborhood of 4 points for optional smooth interpolation
    );
    
    // Interpolates value at a particular proportion of the distance between p1 and p2.
    T interpolate(
            double ofTheWay, // varies between zero and one
            T p0, T p1, T p2, T p3, // neighborhood of 4 points for optional smooth interpolation
            double t0, double t1, double t2, double t3 // x-axis positions of the 4 points
    );
}
