package org.janelia.it.FlyWorkstation.gui.viewer3d;

public class PhysicalDimension 
{
	// The seven SI dimensions
	static public final PhysicalDimension length = new PhysicalDimension();
	static public final PhysicalDimension mass = new PhysicalDimension();
	static public final PhysicalDimension time = new PhysicalDimension();
	static public final PhysicalDimension current = new PhysicalDimension();
	static public final PhysicalDimension temperature = new PhysicalDimension();
	static public final PhysicalDimension amount = new PhysicalDimension();
	static public final PhysicalDimension luminousIntensity = new PhysicalDimension();	
	
	protected PhysicalDimension() {}
}
