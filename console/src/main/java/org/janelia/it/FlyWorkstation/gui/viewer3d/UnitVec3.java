package org.janelia.it.FlyWorkstation.gui.viewer3d;

public class UnitVec3 extends Vec3 
{
	private static final long serialVersionUID = 1L;

	public UnitVec3(CoordinateAxis axis) {
		for (int i = 0; i < 3; ++i) {
			if (i == axis.index()) {
				super.set(i, 1.0);
			}
			else {
				super.set(i, 0.0);				
			}
		}
		super.set(axis.index(), 1.0);
	}
	
	public UnitVec3() {
		this(CoordinateAxis.X);
	}
	
	public UnitVec3(double x, double y, double z) {
		double scale = 1.0 / new Vec3(x, y, z).norm();
		super.set(0, x * scale);
		super.set(1, y * scale);
		super.set(2, z * scale);
	}

	public UnitVec3(Vec3 v) {
		double scale = 1.0 / v.norm();
		super.set(0, v.x() * scale);
		super.set(1, v.y() * scale);
		super.set(2, v.z() * scale);
	}

	/**
	 * Negation is one of the few operations that preserves magnitude
	 */
	public UnitVec3 minus() {
		return new UnitVec3().setElements(-x(), -y(), -z());
	}
	
	protected UnitVec3 setElements(double x, double y, double z) {
		super.set(0, x);
		super.set(1, y);
		super.set(2, z);
		return this;
	}
	
	// turn off methods that might set elements to arbitrary values
	@Override
	public Double set(int index, Double element) {
		throw new UnsupportedOperationException();
	}
	public void setElementAt(Double element, int index) {
		throw new UnsupportedOperationException();
	}
}
