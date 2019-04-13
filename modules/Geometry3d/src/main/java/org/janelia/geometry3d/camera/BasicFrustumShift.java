package org.janelia.geometry3d.camera;

/**
 *
 * @author brunsc
 */
public class BasicFrustumShift implements ConstFrustumShift 
{
    private final float shiftX;
    private final float shiftY;
    
    public BasicFrustumShift(float shiftXMeters, float shiftYMeters) {
        this.shiftX = shiftXMeters;
        this.shiftY = shiftYMeters;
    }
    
    public BasicFrustumShift(ConstFrustumShift rhs) {
        this.shiftX = rhs.getShiftXPixels();
        this.shiftY = rhs.getShiftYPixels();
    }
    
    @Override
    public float getShiftXPixels() {
        return shiftX;
    }

    @Override
    public float getShiftYPixels() {
        return shiftY;
    }
    
}
