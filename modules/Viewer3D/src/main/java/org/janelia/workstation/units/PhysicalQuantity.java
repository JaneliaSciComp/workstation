package org.janelia.workstation.units;

public interface PhysicalQuantity<D extends PhysicalDimension>
{
	PhysicalUnit<D> getUnit();
	double getValue();
}
