package org.janelia.it.FlyWorkstation.gui.viewer3d.camera;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.QtSignal;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.QtSignal1;

public interface ObservableCamera3d
extends Camera3d
{
	public abstract QtSignal getViewChangedSignal();
	QtSignal1<Double> getZoomChanged();
}
