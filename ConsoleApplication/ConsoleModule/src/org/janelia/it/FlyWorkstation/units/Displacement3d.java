package org.janelia.it.FlyWorkstation.units;

public interface Displacement3d {
	PhysicalQuantity<BaseDimension.Length> getX();
	PhysicalQuantity<BaseDimension.Length> getY();
	PhysicalQuantity<BaseDimension.Length> getZ();
}
