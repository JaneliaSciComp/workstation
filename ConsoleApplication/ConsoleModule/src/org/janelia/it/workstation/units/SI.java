package org.janelia.it.workstation.units;

public class SI {

	public static final SIUnit<BaseDimension.Length> micrometer =
			new SIUnit<BaseDimension.Length>(
					"micrometer", 
					"\u00B5"+"m",
					1e-6);

}
