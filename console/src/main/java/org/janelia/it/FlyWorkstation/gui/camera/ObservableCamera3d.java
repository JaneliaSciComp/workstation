package org.janelia.it.FlyWorkstation.gui.camera;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.signal.Signal;
import org.janelia.it.FlyWorkstation.signal.Signal1;

public interface ObservableCamera3d
extends Camera3d
{
	public abstract Signal getViewChangedSignal();
	Signal1<Double> getZoomChangedSignal();
	Signal1<Vec3> getFocusChangedSignal();
}
