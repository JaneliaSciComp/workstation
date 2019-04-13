
package org.janelia.geometry3d;

/**
 *
 * @author cmbruns
 */
public class Quaternion
{
    private final float[] data;
    
    public Quaternion() {
        data = new float[] {0, 0, 0, 1};
    }

    /**
     * 
     * @return reference to internal float array
     */
    public float[] asArray() {
        return data;
    }

    /**
     * Returns [ vx vy vz a ] with (v,a) in canonical form, i.e., 
     * -180 < a <= 180 and |v|=1. 
     * @return 
     */
    float[] convertQuaternionToAxisAngle() {
        float ca2  = data[3];       // cos(a/2)
        Vector3 sa2v = new Vector3(data[0], data[1], data[2]);  // sin(a/2) * v
        float        sa2  = sa2v.length();      // sa2 is always >= 0

        float Eps = (float)1e-7;
        float Pi  = (float)Math.PI;

        // TODO: what is the right value to use here?? Norms can be
        // much less than eps and still OK -- this is 1e-32 in double.
        if( sa2 < square(Eps) )  return new float[]{ 1,0,0,0 }; // no rotation, x axis

        // Use atan2.  Do NOT just use acos(q[0]) to calculate the rotation angle!!!
        // Otherwise results are numerical garbage anywhere near zero (or less near).
        float angle = 2 * (float)Math.atan2(sa2,ca2);

        // Since sa2>=0, atan2 returns a value between 0 and pi, which is then
        // multiplied by 2 which means the angle is between 0 and 2pi.
        // We want an angle in the range:  -pi < angle <= pi range.
        // E.g., instead of rotating 359 degrees clockwise, rotate -1 degree counterclockwise.
        if( angle > Pi ) angle -= 2*Pi;

        // Normalize the axis part of the return value
        Vector3 axis = new Vector3(sa2v).multiplyScalar(1.0f/sa2);

            // Return (angle/axis)
        return new float[] { axis.getX(), axis.getY(), axis.getZ(), angle };
    }
    
    public Quaternion set(float x, float y, float z, float w) {
        data[0] = x;
        data[1] = y;
        data[2] = z;
        data[3] = w;
        return this;
    }
    
    public Quaternion setFromAxisAngle(ConstVector3 axis, float theta) {
        float ca2 = (float)Math.cos(theta/2), sa2 = (float)Math.sin(theta/2);
        // Multiplying an entire quaternion by -1 produces the same Rotation matrix
        // (each element of the Rotation element involves the product of two quaternion elements).
        // The canonical form is to make the first element of the quaternion positive.
        if( ca2 < 0 ) { ca2 = -ca2; sa2 = -sa2; }
        data[3] = ca2;
        float[] v = axis.toNewArray();
        for (int i = 0; i < 3; ++i)
            data[i] = v[i] * sa2;
        return this;
    }
    
    public Quaternion slerp(Quaternion rhs, float alpha) {
        return slerp(rhs, alpha, 0.0f);
    }
    
    public Quaternion slerp(Quaternion rhs, float alpha, float spin) 
    {
        /**
        Spherical linear interpolation of quaternions.
        From page 448 of "Visualizing Quaternions" by Andrew J. Hanson
         */
        float cos_t = 0;
        float qA[] = data;
        float qB[] = rhs.data;
        for (int i = 0; i < 4; ++i)
            cos_t += qA[i] * qB[i];
        // If qB is on opposite hemisphere from qA, use -qB instead
        boolean bFlip = false;
        if (cos_t < 0.0) {
            cos_t = -cos_t;
            bFlip = true;
        }
        // If qB is the same as qA (within precision)
        // just linear interpolate between qA and qB.
        // Can't do spins, since we don't know what direction to spin.
        float beta;
        if ((1.0 - cos_t) < 1e-7) {
            beta = 1.0f - alpha;
        }
        else { // normal case
            float theta = (float)Math.acos(cos_t);
            float phi = theta + spin * (float)Math.PI;
            float sin_t = (float)Math.sin(theta);
            beta = (float)Math.sin(theta - alpha * phi) / sin_t;
            alpha = (float)Math.sin(alpha * phi) / sin_t;
        }
        if (bFlip)
            alpha = -alpha;
        // interpolate
        float result[] = {0,0,0,0};
        for (int i = 0; i < 4; ++i) {
            result[i] = beta*qA[i] + alpha*qB[i];
        }
        return new Quaternion().set(result[0], result[1], result[2], result[3]);
    }
    
    
    private float square(float a) {return a*a;}

}
