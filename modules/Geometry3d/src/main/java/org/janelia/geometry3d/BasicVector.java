package org.janelia.geometry3d;

import java.util.Arrays;

/**
 *
 * @author Christopher Bruns
 */
public class BasicVector implements ConstVector 
{
    protected final float[] data;
    
    BasicVector(int size) {
        data = new float[size];
    }

    /**
     * Copy constructor, to avoid broken Java clone() approach.
     * @param cloned 
     */
    private BasicVector(BasicVector cloned) {
        data = cloned.data.clone();
    }
    
    BasicVector(ConstVector cloned) {
        data = new float[cloned.size()];
        for (int i = 0; i < data.length; ++i)
            data[i] += cloned.get(i);
    }
    
    public float[] toArray() {
        return data;
    }

    @Override
    public float[] toNewArray() {
        return Arrays.copyOf(data, data.length);
    }

    // TODO unchecked size
    @Override
    public float dot(ConstVector rhs) {
        float result = 0;
        for (int i = 0; i < data.length; ++i)
            result += data[i]*rhs.get(i);
        return result;
    }
    
    @Override
    public float distance(ConstVector rhs) {
        return (float)Math.sqrt(this.distanceSquared(rhs));
    }

    @Override
    public float distanceSquared(ConstVector rhs) {
        BasicVector v = new BasicVector(this).sub(rhs);
        return v.dot(v);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BasicVector other = (BasicVector) obj;
        return Arrays.equals(this.data, other.data);
    }

    @Override
    public float get(int i) {
        return data[i];
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    public void set(int ix, float v) {
        data[ix] = v;
    }
    
    @Override
    public int size() {
        return data.length;
    }

    public BasicVector sub(ConstVector rhs) {
        for (int i = 0; i < data.length; ++i)
            data[i] -= rhs.get(i);
        return this;
    }
    
    @Override
    public String toString() {
        return Arrays.toString(data);
    }

}
