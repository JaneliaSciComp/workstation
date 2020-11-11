package org.janelia.geometry3d;

/**
 *
 * @author brunsc
 */
public class Vector3 extends BasicVector implements ConstVector3 {

    public Vector3(float x, float y, float z) {
        super(3);
        data[0] = x;
        data[1] = y;
        data[2] = z;
    }

    public Vector3(float[] xyz) {
        super(3);
        data[0] = xyz[0];
        data[1] = xyz[1];
        data[2] = xyz[2];
    }

    public Vector3(Double x, Double y, Double z) {
        super(3);
        data[0] = x.floatValue();
        data[1] = y.floatValue();
        data[2] = z.floatValue();
    }

    /**
     * Copy constructor, to avoid broken Java clone() approach.
     * @param cloned 
     */
    public Vector3(ConstVector3 cloned) {
        super(cloned);
    }
    
    public Vector3 applyRotation(Rotation r) {
        float[] p = data.clone(); // copy before overwrite
        float[] R = r.asArray();
        for (int i = 0; i < 3; ++i) {
            data[i] = 0;
            for (int j = 0; j < 3; ++j) {
                data[i] += R[3*i+j] * p[j];
            }
        }   
        return this;
    }
    
    public Vector3 add(ConstVector3 rhs) {
        for (int i = 0; i < 3; ++i)
            data[i] += rhs.get(i);
        return this;
    }
    
    public Vector3 plus(ConstVector3 rhs) {
        return new Vector3(this).add(rhs);
    }

    public Vector3 minus(ConstVector3 rhs) {
        Vector3 result = new Vector3(this);
        result.sub(rhs);
        return result;
    }

    public void copy(Vector3 rhs) {
        System.arraycopy(rhs.data, 0, data, 0, 3);
    }
    
    @Override
    public Vector3 cross(ConstVector3 rhs) {
        float x = getY()*rhs.getZ() - getZ()*rhs.getY();
        float y = getZ()*rhs.getX() - getX()*rhs.getZ();
        float z = getX()*rhs.getY() - getY()*rhs.getX();
        return new Vector3(x, y, z);
    }
    
    @Override
    public float getX() {return data[0];}
    @Override
    public float getY() {return data[1];}
    @Override
    public float getZ() {return data[2];}
    
    /**
     * Inverts this vector.
     * @return this vector
     */
    public Vector3 negate() {
        return multiplyScalar(-1);
    }

    @Override
    public float length() {
        return (float)Math.sqrt(lengthSquared());
    }
    
    public Vector3 normalize() {
        float scale = 1.0f/length();
        return this.multiplyScalar(scale);
    }
    
    @Override
    public float lengthSquared() {
        return this.dot(this);
    }
    
    /**
     * Multiplies this vector by a scalar s
     * @param s
     * @return 
     */
    public Vector3 multiplyScalar(float s) {
        for (int i = 0; i < 3; ++i)
            data[i] *= s;
        return this;    
    }

    /**
     * Creates a Matrix4, representing this Vector3 as a translation.
     * @return 
     */
    @Override
    public Matrix4 asTransform() {
        return new Matrix4(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                data[0], data[1], data[2], 1);
    }

    public void set(float x, float y, float z) {
        data[0] = x;
        data[1] = y;
        data[2] = z;
    }
    
    public Vector3 setX(float x) {
        data[0] = x;
        return this;
    }
    public Vector3 setY(float y) {
        data[1] = y;
        return this;
    }
    public Vector3 setZ(float z) {
        data[2] = z;
        return this;
    }

    public Vector3 sub(Vector3 rhs) {
        super.sub(rhs);
        return this;
    }
}
