package org.janelia.it.FlyWorkstation.gui.animate;

import org.janelia.it.FlyWorkstation.geom.Quaternion;

/**
 * Interpolator of discreet values that uses continuous values internally.
 * 
 * @author brunsc
 *
 */
public class QuaternionInterpolator 
{
    private Quaternion value;
    
    public QuaternionInterpolator(Quaternion value) {
        this.value = value;
    }

    public Quaternion catmullRomInterpolate(
            Quaternion q00, Quaternion q02,
            Quaternion q03, double t) 
    {
        Quaternion q01 = retrieve();
        // From method CatmullQuat(...) on page 449 of Visualizing Quaternions
        Quaternion q10 = q00.slerp(q01, t+1.0);
        Quaternion q11 = q01.slerp(q02, t+0.0);
        Quaternion q12 = q02.slerp(q03, t-1.0);
        Quaternion q20 = q10.slerp(q11, (t+1.0)/2.0);
        Quaternion q21 = q11.slerp(q12, t/2.0);
        return q20.slerp(q21, t);
    }

    /**
     * Spherical linear interpolation
     * @param p2 the other Quaternion
     * @param alpha the interpolation parameter, range 0-1
     * @return
     */
    public Quaternion linearInterpolate(Quaternion p2,
            double alpha, double spin) 
    {
        return value.slerp(p2, alpha, spin);
    }

    public Quaternion retrieve() {
        // Is interpolated value closer to true or false?
        return value;
    }

}
