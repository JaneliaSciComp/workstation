package org.janelia.it.FlyWorkstation.units;

public interface PhysicalQuantity<D extends PhysicalDimension> 
{
	PhysicalUnit<D> getUnit();
	double getValue();
}
