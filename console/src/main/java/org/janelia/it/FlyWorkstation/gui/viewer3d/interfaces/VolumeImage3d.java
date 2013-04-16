package org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces;

import java.net.URL;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

public interface VolumeImage3d 
{
	public BoundingBox3d getBoundingBox3d();
	public int getMaximumIntensity(); // e.g. "255" for 8-bit images
	// public double getMaximumResolution(); // in scene units
	public double getXResolution(); // in scene units
	public double getYResolution(); // in scene units
	public double getZResolution(); // in scene units
	public int getNumberOfChannels(); // e.g. "3" for RGB
	public boolean loadURL(URL url);
}
