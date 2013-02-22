package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

public interface VolumeViewer 
extends Camera3d, VolumeImage3d, Viewport
{
	double getMaxZoom();
	double getMinZoom();

	QtSignal1<Double> getZoomChangedSignal();
}
