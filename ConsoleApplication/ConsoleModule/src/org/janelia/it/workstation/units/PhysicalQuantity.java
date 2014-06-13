package org.janelia.it.workstation.units;

public interface PhysicalQuantity<D extends PhysicalDimension>
{
	PhysicalUnit<D> getUnit();
	double getValue();
}
