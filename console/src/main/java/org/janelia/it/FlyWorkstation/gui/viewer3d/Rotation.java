package org.janelia.it.FlyWorkstation.gui.viewer3d;

public class Rotation extends SizedVector<UnitVec3> 
{
	private static final long serialVersionUID = 1L;

	public Rotation() {
		super(3);
		super.set(0, new UnitVec3(CoordinateAxis.X));
		super.set(1, new UnitVec3(CoordinateAxis.Y));
		super.set(2, new UnitVec3(CoordinateAxis.Z));
	}

	public double get(int i, int j) {
		return get(i).get(j);
	}

    public Rotation inverse() {
        return this.transpose();
    }

    protected Rotation setElements(
			double e00, double e01, double e02,
			double e10, double e11, double e12,
			double e20, double e21, double e22) 
	{
		super.get(0).setElements(e00, e01, e02);
		super.get(1).setElements(e10, e11, e12);
		super.get(2).setElements(e20, e21, e22);
		return this;
	}
	
	/**
	 * Rotate counterclockwise a multiple of 90 degrees about a principal axis.
	 * @param quadrantCount
	 * @param axis
	 * @return
	 */
	public Rotation setFromCanonicalRotationAboutPrincipalAxis(int quadrantCount, CoordinateAxis axis)
	{
	    UnitVec3 uv = new UnitVec3(axis);
	    double angle = 0.5 * Math.PI * quadrantCount;
	    return setFromAngleAboutUnitVector(angle, uv);
	}
	
	public Rotation setFromAngleAboutUnitVector(double angle, UnitVec3 axis) {
		Quaternion q = new Quaternion(angle, axis);
		setFromQuaternion(q);
		if (new Double(get(0,0)).isNaN()) {
			System.out.println("isnan");
		}
		return this;
	}
	
	public Rotation setFromQuaternion(Quaternion q) {
		double q0 = q.w();
		double q1 = q.x();
		double q2 = q.y();
		double q3 = q.z();
		double q00, q11, q22, q33, q01, q02, q03, q12, q13, q23;
        q00=q0*q0; q11=q1*q1; q22=q2*q2; q33=q3*q3;
        q01=q0*q1; q02=q0*q2; q03=q0*q3;
        q12=q1*q2; q13=q1*q3; q23=q2*q3;
        setElements(q00+q11-q22-q33,   2.0*(q12-q03),   2.0*(q13+q02),
                     2.0*(q12+q03) , q00-q11+q22-q33,   2.0*(q23-q01),
                     2.0*(q13-q02) ,   2.0*(q23+q01)  , q00-q11-q22+q33);
		return this;
	}

	public Rotation times(Rotation rhs) {
        Rotation result = new Rotation();
		int[] indices = {0, 1, 2};
		for (int i : indices) {
			double[] row = {0.0, 0.0, 0.0};
			for (int j : indices) {
				for (int k : indices) {
					row[j] += get(i, k) * rhs.get(k, j);
				}
			}
			result.get(i).setElements(row[0], row[1], row[2]);
		}
		if (new Double(result.get(0,0)).isNaN()) {
			System.out.println("isnan");
		}
		return result;
	}
	
	public Vec3 times(Vec3 rhs) {
		Vec3 result = new Vec3(0,0,0);
		for (int i = 0; i < 3; ++i) {
			result.set(i, rhs.dot(get(i)));
		}
		return result;
	}
	
	public String toString() {
		return "["
			+get(0).toString()+",\n"
			+get(1).toString()+",\n"
			+get(2).toString()+"]";
	}
	
	public Rotation transpose() {
		return new Rotation().setElements(
				get(0,0), get(1,0), get(2,0),
				get(0,1), get(1,1), get(2,1),
				get(0,2), get(1,2), get(2,2));
	}
	
	// turn of methods that might set elements to arbitrary values
	@Override
	public UnitVec3 set(int index, UnitVec3 element) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void setElementAt(UnitVec3 element, int index) {
		throw new UnsupportedOperationException();
	}

    /**
     * AT time of writing, this is designed for use only with deserialized values.  Values across all three vectors
     * handed in through three calls to this method, must be sum-of-squares==1, dot-product-any-row-col==0,
     * determinant==+1.
     *
     * Therefore, only call this when those conditions are met, and then call with all three vectors, serially.
     *
     * @author fosterl@janelia.hhmi.org
     * @param index which of the three vectors to set.
     * @param element a unit vector for the chosen position.
     */
    public void setWithCaution(int index, UnitVec3 element) {
        super.get( index ).setElements( element.getX(), element.getY(), element.getZ() );

    }
}
