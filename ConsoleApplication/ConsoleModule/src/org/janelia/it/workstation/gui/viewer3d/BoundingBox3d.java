package org.janelia.it.workstation.gui.viewer3d;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.units.BaseDimension.*;
import org.janelia.it.workstation.units.PhysicalUnit;
import org.janelia.it.workstation.units.SI;

public class BoundingBox3d 
{
	protected Vec3 min = new Vec3(Double.NaN, Double.NaN, Double.NaN);
	protected Vec3 max = new Vec3(Double.NaN, Double.NaN, Double.NaN);
	protected PhysicalUnit<Length> physicalUnit = SI.micrometer;

	public Vec3 clip(Vec3 point) {
		Vec3 result = new Vec3(point.x(), point.y(), point.z());
	    for (int i = 0; i < 3; ++i) {
	        double x = result.get(i);
	        double min = this.getMin().get(i);
	        double max = this.getMax().get(i);
	        if (x < min)
	            result.set(i, min);
	        if (x > max)
	            result.set(i, max);
	    }
	    return result;
	}
	
	public Vec3 getCenter() {
		return max.plus(min).times(0.5);
	}
	
	public double getDepth() {
		return max.z() - min.z();
	}
	
	public double getHeight() {
		return max.y() - min.y();
	}
	
    public Vec3 getMax() {
		return max;
	}

    public double getMaxX() {return max.getX();}
    public double getMaxY() {return max.getY();}
    public double getMaxZ() {return max.getZ();}

	public Vec3 getMin() {
		return min;
	}

    public double getMinX() {return min.getX();}
    public double getMinY() {return min.getY();}
    public double getMinZ() {return min.getZ();}

	public double getWidth() {
		return max.x() - min.x();
	}
	
	/**
	 * Expand this bounding box to contain another bounding box.
	 * @param rhs
	 */
	public void include(BoundingBox3d rhs) {
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
		// NOTE weird comparisons because there might be NaNs
		// Compare to old min values
		if ( (!Double.isNaN(x)) && (! (x >= min.x())) )
			min.setX(x);
		if ( (!Double.isNaN(y)) && (! (y >= min.y())) )
			min.setY(y);
		if ( (!Double.isNaN(z)) && (! (z >= min.z())) )
			min.setZ(z);
		// Compare to old max values
		if ( (!Double.isNaN(x)) && (! (x <= max.x())) )
			max.setX(x);
		if ( (!Double.isNaN(y)) && (! (y <= max.y())) )
			max.setY(y);
		if ( (!Double.isNaN(z)) && (! (z <= max.z())) )
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

	public void setMax(double x, double y, double z) {
		this.max = new Vec3(x, y, z);
	}
	
	public void setMax(Vec3 max) {
		this.max = max;
	}

	public void setMin(double x, double y, double z) {
		this.min = new Vec3(x, y, z);
	}

	public void setMin(Vec3 min) {
		this.min = min;
	}

}
