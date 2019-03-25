package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d;

public interface VolumeViewer 
extends Camera3d, VolumeImage3d, Viewport
{
	double getMaxZoom();
	double getMinZoom();
}
