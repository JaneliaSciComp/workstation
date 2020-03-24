package org.janelia.workstation.gui.camera;

import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.signal.Signal;
import org.janelia.workstation.signal.Signal1;

public interface ObservableCamera3d
extends Camera3d
{
	public abstract Signal getViewChangedSignal();
	Signal1<Double> getZoomChangedSignal();
	Signal1<Vec3> getFocusChangedSignal();
}
