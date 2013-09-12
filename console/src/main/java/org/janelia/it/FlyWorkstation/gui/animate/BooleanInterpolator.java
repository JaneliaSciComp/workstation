package org.janelia.it.FlyWorkstation.gui.animate;

/**
 * Interpolator of discreet values that uses continuous values internally.
 * 
 * @author brunsc
 *
 */
public class BooleanInterpolator 
{
    private DoubleInterpolator doubleValue;
    
    public BooleanInterpolator(boolean value) {
        double d = 0.0; // zero means false
        if (value)
            d = 1.0; // one means true
        doubleValue = new DoubleInterpolator(d);
    }

    protected BooleanInterpolator(double doubleValue) {
        this.doubleValue = new DoubleInterpolator(doubleValue);
    }
    
    public BooleanInterpolator catmullRomInterpolate(
            BooleanInterpolator i0, BooleanInterpolator i2,
            BooleanInterpolator i3, double t) 
    {
        double result = doubleValue.catmullRomInterpolate(
                i0.getDoubleValue(), 
                i2.getDoubleValue(), 
                i3.getDoubleValue(), 
                t);
        return new BooleanInterpolator(result);
    }

    protected double getDoubleValue() {return doubleValue.retrieve();}
    
    public BooleanInterpolator linearInterpolate(BooleanInterpolator p2,
            double alpha) {
        double beta = 1.0 - alpha;
        return new BooleanInterpolator(beta * getDoubleValue() + alpha * p2.getDoubleValue());
    }

    public boolean retrieve() {
        // Is interpolated value closer to true or false?
        return (getDoubleValue() >= 0.5);
    }

}
