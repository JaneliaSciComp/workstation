package org.janelia.it.FlyWorkstation.units;

public class SIUnit<D extends PhysicalDimension> implements PhysicalUnit<D> 
{
	private String name;
	private String symbol;
	private double conversionFactor;

	public SIUnit(String name, String symbol, double conversionFactor)
	{
		this.name = name;
		this.symbol = symbol;
		this.conversionFactor = conversionFactor;
	}

	@Override
	public String getSymbol() {
		return symbol;
	}
	
	@Override 
	public String toString() {
		return name;
	}
}
