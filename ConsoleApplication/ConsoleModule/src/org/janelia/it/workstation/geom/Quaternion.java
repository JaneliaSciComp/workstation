package org.janelia.it.workstation.geom;

public class Quaternion {
	private double[] q = new double[4];
	
	public Quaternion() {
		setElements(1,0,0,0);
	}
	
	public Quaternion(double angle, UnitVec3 axis) {
		double ca2 = Math.cos(angle/2.0);
		double sa2 = Math.sin(angle/2.0);
		if (ca2 < 0.0) {
			ca2 = -ca2;
			sa2 = -sa2;
		}
		setElements(ca2, sa2*axis.x(), sa2*axis.y(), sa2*axis.z());
	}
	
	public Quaternion(double q0, double q1, double q2, double q3, boolean isAlreadyNormalized)
	{
		if (isAlreadyNormalized)
			setElements(q0, q1, q2, q3);
		else {
		    double scale = 0.0; // q.norm();
		    double q[] = {q0, q1, q2, q3};
		    for (int i = 0; i < 4; ++i)
		    	scale += q[i]*q[i];
		    scale = Math.sqrt(scale);
		    if( q[0] < 0 )  scale = -scale;   // canonicalize
		    scale = 1.0 / scale;
		    for (int i = 0; i < 4; ++i)
		    	q[i] = q[i] * scale;
		    setElements(q[0], q[1], q[2], q[3]);
		}
	}
	
	public AngleAxis convertQuaternionToAngleAxis() {
	    double ca2  = q[0];       // cos(a/2)
	    Vec3 sa2v = new Vec3(q[1], q[2], q[3]);  // sin(a/2) * v
	    double sa2  = sa2v.norm();      // sa2 is always >= 0

	    // TODO: what is the right value to use here?? Norms can be
	    // much less than eps and still OK -- this is 1e-32 in double.
	    if( sa2 < 1e-20 )  return new AngleAxis(0,new UnitVec3(1,0,0)); // no rotation, x axis

	    // Use atan2.  Do NOT just use acos(q[0]) to calculate the rotation angle!!!
	    // Otherwise results are numerical garbage anywhere near zero (or less near).
	    double angle = 2.0 * Math.atan2(sa2,ca2);

	    // Since sa2>=0, atan2 returns a value between 0 and pi, which is then
	    // multiplied by 2 which means the angle is between 0 and 2pi.
	    // We want an angle in the range:  -pi < angle <= pi range.
	    // E.g., instead of rotating 359 degrees clockwise, rotate -1 degree counterclockwise.
	    while ( angle > Math.PI ) angle -= 2*Math.PI;

	    // Normalize the axis part of the return value
	    UnitVec3 axis = new UnitVec3(sa2v);

	    // Return (angle/axis)
	    return new AngleAxis(angle, axis);
	}
	
	private void setElements(double w, double x, double y, double z) {
		q[0] = w;
		q[1] = x;
		q[2] = y;
		q[3] = z;
	}
	
    /**
     * Spherical linear interpolation
     * @param p2 the other Quaternion
     * @param alpha the interpolation parameter, range 0-1
     * @return
     */
    public Quaternion slerp(Quaternion p2, double alpha) {
        return slerp(p2, alpha, 0.0);
    }
    
    public Quaternion slerp(Quaternion p2, double alpha, double spin) 
    {
        /**
        Spherical linear interpolation of quaternions.
        From page 448 of "Visualizing Quaternions" by Andrew J. Hanson
         */
        double cos_t = 0;
        double qA[] = q;
        double qB[] = p2.q;
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
        double beta;
        if ((1.0 - cos_t) < 1e-7) {
            beta = 1.0 - alpha;
        }
        else { // normal case
            double theta = Math.acos(cos_t);
            double phi = theta + spin * 3.14159;
            double sin_t = Math.sin(theta);
            beta = Math.sin(theta - alpha * phi) / sin_t;
            alpha = Math.sin(alpha * phi) / sin_t;
        }
        if (bFlip)
            alpha = -alpha;
        // interpolate
        double result[] = {0,0,0,0};
        for (int i = 0; i < 4; ++i) {
            result[i] = beta*qA[i] + alpha*qB[i];
        }
        return new Quaternion(result[0], result[1], result[2], result[3], false);
    }

	public double w() {return q[0];}
	public double x() {return q[1];}
	public double y() {return q[2];}
	public double z() {return q[3];}
	
	public class AngleAxis {
		public AngleAxis(double angle, UnitVec3 axis) {
			this.angle = angle;
			this.axis = axis;
		}
		public double angle;
		public UnitVec3 axis;
	}
}
