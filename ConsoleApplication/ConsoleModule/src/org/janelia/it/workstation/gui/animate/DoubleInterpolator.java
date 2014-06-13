package org.janelia.it.workstation.gui.animate;

/**
 * Simplest interpolator, base on continuous scalar floating point values.
 * 
 * @author brunsc
 *
 */
public class DoubleInterpolator 
{
    private double value;
    
    public DoubleInterpolator(double value) {
        this.value = value;
    }

    public double catmullRomInterpolate(
            double p0, double p2,
            double p3, double t) 
    {
        double p1 = value;
        
        double t2 = t*t;
        double t3 = t2*t;
        double interp = 0.5 * ( (2.0*p1)
                       + (-p0 + p2) * t
                       + (2.0*p0 - 5.0*p1 + 4.0*p2 - p3) * t2
                       + (-p0 + 3.0*(p1 - p2) + p3) * t3 );
        return interp;
    }

    public double linearInterpolate(double p2, double alpha) {
        double beta = 1.0 - alpha;
        return beta * value + alpha * p2;
    }

    public double retrieve() {
        return value;
    }
}
