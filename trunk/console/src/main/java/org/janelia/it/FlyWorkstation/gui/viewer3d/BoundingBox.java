package org.janelia.it.FlyWorkstation.gui.viewer3d;

public class BoundingBox {
	public Vec3 min = new Vec3(Double.NaN, Double.NaN, Double.NaN);
	public Vec3 max = new Vec3(Double.NaN, Double.NaN, Double.NaN);

	public Vec3 getCenter() {
		return max.plus(min).times(0.5);
	}
	
	public double getHeight() {
		return max.y() - min.y();
	}
	
	public double getWidth() {
		return max.x() - min.x();
	}
	
	public double getDepth() {
		return max.z() - min.z();
	}
	
	/**
	 * Expand this bounding box to contain another bounding box.
	 * @param rhs
	 */
	public void include(BoundingBox rhs) {
		include(rhs.min);
		include(rhs.max);
	}
	
	/**
	 * Expand this bounding box to include the given point.
	 * @param v
	 */
	public void include(Vec3 v) 
	{
		double x = v.x();
		double y = v.y();
		double z = v.z();
		// weird comparisons because there might be NaNs
		if ( (!Double.isNaN(x)) && (! (x >= min.x())) )
			min.setX(x);
		if ( (!Double.isNaN(y)) && (! (y >= min.y())) )
			min.setY(y);
		if ( (!Double.isNaN(z)) && (! (z >= min.z())) )
			min.setZ(z);

		if ( (!Double.isNaN(x)) && (! (x <= min.x())) )
			max.setX(x);
		if ( (!Double.isNaN(y)) && (! (y <= min.y())) )
			max.setY(y);
		if ( (!Double.isNaN(z)) && (! (z <= min.z())) )
			max.setZ(z);
	}
	
	public boolean isEmpty() {
		if (Double.isNaN(min.x())) return true;
		if (Double.isNaN(min.y())) return true;
		if (Double.isNaN(min.z())) return true;
		if (Double.isNaN(max.x())) return true;
		if (Double.isNaN(max.y())) return true;
		if (Double.isNaN(max.z())) return true;		
		return false;
	}
}
