package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.janelia.it.FlyWorkstation.signal.Signal1;

public interface VolumeViewer 
extends Camera3d, VolumeImage3d, Viewport
{
	double getMaxZoom();
	double getMinZoom();

	Signal1<Double> getZoomChangedSignal();
}
