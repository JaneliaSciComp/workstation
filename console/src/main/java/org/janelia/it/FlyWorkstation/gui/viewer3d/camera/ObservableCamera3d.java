package org.janelia.it.FlyWorkstation.gui.viewer3d.camera;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.Signal;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.Signal1;

public interface ObservableCamera3d
extends Camera3d
{
	public abstract Signal getViewChangedSignal();
	Signal1<Double> getZoomChangedSignal();
	Signal1<Vec3> getFocusChangedSignal();
}
