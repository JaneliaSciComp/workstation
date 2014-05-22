package org.janelia.it.workstation.units;

/**
 * Physical unit like "meter" or "kilogram" or "meters per second"
 * 
 * @author brunsc
 *
 * @param <D>
 */
public interface PhysicalUnit<D extends PhysicalDimension> {
	String toString(); // name of unit
	String getSymbol(); // symbol abbreviation
}
