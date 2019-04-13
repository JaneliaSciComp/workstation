package org.janelia.geometry3d.camera;

/**
 *
 * @author brunsc
 */
public class BasicViewSlab implements ConstViewSlab
{
    private final float zNear, zFar;
    
    public BasicViewSlab(ConstViewSlab template) {
        this.zNear = template.getzNearRelative();
        this.zFar = template.getzFarRelative();
    }

    public BasicViewSlab(float zNearRelative, float zFarRelative) {
        this.zNear = zNearRelative;
        this.zFar = zFarRelative;
    }

    @Override
    public float getzNearRelative() {
        return zNear;
    }

    @Override
    public float getzFarRelative() {
        return zFar;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + Float.floatToIntBits(this.zNear);
        hash = 47 * hash + Float.floatToIntBits(this.zFar);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (! (obj instanceof ConstViewSlab))
            return false;
        final ConstViewSlab other = (ConstViewSlab) obj;
        if (zNear != other.getzNearRelative())
            return false;
        if (zFar != other.getzFarRelative())
            return false;
        return true;
    }
    
}
