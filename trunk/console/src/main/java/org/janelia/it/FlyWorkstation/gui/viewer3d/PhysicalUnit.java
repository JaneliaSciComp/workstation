package org.janelia.it.FlyWorkstation.gui.viewer3d;

public interface PhysicalUnit {
	double getConversionFactorTo(PhysicalUnit other);
	double getConversionFactorToSI();
	String getName();
	String getSymbol();
}
