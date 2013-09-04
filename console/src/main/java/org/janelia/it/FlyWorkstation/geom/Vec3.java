package org.janelia.it.FlyWorkstation.geom;


// Vec3 is an XYZ point or displacement in three dimensional space.
// Units are unspecified.
public class Vec3 extends SizedVector<Double>
{
	private static final long serialVersionUID = 1L;

	public Vec3() {
		super(3);
	}
	
	public Vec3(double x, double y, double z) {
		super(3);
		this.set(0, x);
		this.set(1, y);
		this.set(2, z);
	}
	
	@Override
    public synchronized Vec3 clone() {
    	return new Vec3(get(0), get(1), get(2));
    }
    
	public double dot(Vec3 rhs) {
		double result = 0.0f;
		for (int i = 0; i < 3; ++i) {
			result += this.get(i) * rhs.get(i);
		}
		return result;
	}
	
	public double getX() {
		return get(0);
	}

	public double getY() {
		return get(1);
	}

	public double getZ() {
		return get(2);
	}

	public Vec3 minus() {
		return new Vec3(-x(), -y(), -z());
	}
	
	public Vec3 minus(Vec3 rhs) {
		return new Vec3(
				x() - rhs.x(), 
				y() - rhs.y(), 
				z() - rhs.z());
	}
	
	public void multEquals(double d) {
		for (int i = 0; i < 3; ++i) {
			this.set(i, this.get(i)*d);
		}
	}

	public double norm() {
		return Math.sqrt(this.normSqr());
	}
	
	public double normSqr() {
		return this.dot(this);
	}
	
	public Vec3 plus(Vec3 rhs) {
		return new Vec3(
				x() + rhs.x(),
				y() + rhs.y(),
				z() + rhs.z());
	}
	
	public void plusEquals(Vec3 rhs) {
		set(0, x() + rhs.x());
		set(1, y() + rhs.y());
		set(2, z() + rhs.z());
	}
	
	public void setX(double x) {
		set(0, x);
	}
	
	public void setY(double y) {
		set(1, y);
	}
	
	public void setZ(double z) {
		set(2, z);
	}
	
	public Vec3 times(double rhs) {
		return new Vec3(x()*rhs, y()*rhs, z()*rhs);
	}
	
	public String toString()
	{
		return "Vec3("
			+ get(0).toString() + ", "
			+ get(1).toString() + ", "
			+ get(2).toString() + ")"
			;
	}
	
	public double x() {
		return get(0);
	}
	
	public double y() {
		return get(1);
	}
	
	public double z() {
		return get(2);
	}

}
