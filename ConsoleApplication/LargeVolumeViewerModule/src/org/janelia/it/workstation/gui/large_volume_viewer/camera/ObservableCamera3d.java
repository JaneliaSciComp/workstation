package org.janelia.it.workstation.gui.large_volume_viewer.camera;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraListener;
//import org.janelia.it.workstation.signal.Signal;
//import org.janelia.it.workstation.signal.Signal1;

public interface ObservableCamera3d
extends Camera3d
{
    void addCameraListener(CameraListener listener);
    void removeCameraListener(CameraListener listener);
    void fireZoomChanged(Double newZoom);
    void fireFocusChanged(Vec3 newFocus);
    void fireViewChanged();
//	Signal getViewChangedSignal();
//	Signal1<Double> getZoomChangedSignal();
//	Signal1<Vec3> getFocusChangedSignal();
}
