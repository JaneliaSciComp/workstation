package org.janelia.it.workstation.units;

public interface PhysicalQuantity<D extends PhysicalDimension>
{
	org.janelia.it.workstation.units.PhysicalUnit<D> getUnit();
	double getValue();
}
