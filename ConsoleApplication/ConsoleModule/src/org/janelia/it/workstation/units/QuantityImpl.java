package org.janelia.it.workstation.units;

public class QuantityImpl<D extends PhysicalDimension>
implements PhysicalQuantity<D>
{
	private double value;
	private PhysicalUnit<D> unit;
	
	public QuantityImpl(double value, PhysicalUnit<D> unit) {
		this.value = value;
		this.unit = unit;
	}

	@Override
	public PhysicalUnit<D> getUnit() {
		return unit;
	}

	@Override
	public double getValue() {
		return value;
	}

}
