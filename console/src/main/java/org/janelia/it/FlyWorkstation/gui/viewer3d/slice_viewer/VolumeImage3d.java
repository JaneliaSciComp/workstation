package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

public interface VolumeImage3d 
{
	public BoundingBox3d getBoundingBox3d();
	public double getMaxResolution(); // in scene units
}
