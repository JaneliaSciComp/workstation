package org.janelia.it.FlyWorkstation.gui.viewer3d;

public class BasicPhysicalUnit implements PhysicalUnit 
{
	protected PhysicalDimension dimension;
	protected String name;
	protected String symbol;
	protected double conversionFactorToSI;

	public BasicPhysicalUnit(
			PhysicalDimension dimension,
			String name,
			String symbol,
			double conversionFactorToSI)
	{
		this.dimension = dimension;
		this.name = name;
		this.symbol = symbol;
		this.conversionFactorToSI = conversionFactorToSI;

	}
	
	@Override
	public double getConversionFactorTo(PhysicalUnit other) {
		return this.getConversionFactorToSI() / other.getConversionFactorToSI();
	}

	public PhysicalDimension getDimension() {
		return dimension;
	}

	public double getConversionFactorToSI() {
		return conversionFactorToSI;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSymbol() {
		return symbol;
	}

}
