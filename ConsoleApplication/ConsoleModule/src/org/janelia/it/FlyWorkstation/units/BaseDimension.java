package org.janelia.it.FlyWorkstation.units;

/**
 * Fundamental physical dimension, for use in units-aware scientific computation.
 * 
 * @author brunsc
 *
 */
public class BaseDimension implements PhysicalDimension 
{
	static final public class Length extends BaseDimension {};
	static final public class Mass extends BaseDimension {};
	static final public class Time extends BaseDimension {};
	
	/**
	 * Protected constructor intended to discourage clients from conjuring 
	 * their own dimensions; because there should not be many.
	 */
	protected BaseDimension() {}
}
