package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

public interface VolumeImage3d 
{
	public BoundingBox3d getBoundingBox3d();
	public double getMaxResolution(); // in scene units
	public int getNumberOfChannels(); // e.g. "3" for RGB
	public int getMaximumIntensity(); // e.g. "255" for 8-bit images
}
