package org.janelia.it.workstation.gui.large_volume_viewer.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.BasicCamera3d;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraListener;
//import org.janelia.it.workstation.signal.Signal;
//import org.janelia.it.workstation.signal.Signal1;

public class BasicObservableCamera3d 
extends Observable 
implements Camera3d, ObservableCamera3d
{
	private Camera3d camera = new BasicCamera3d();
//	private Signal viewChangedSignal = new Signal();
//	private Signal1<Double> zoomChangedSignal = new Signal1<Double>();
//	private Signal1<Vec3> focusChangedSignal = new Signal1<Vec3>();
    
    private List<CameraListener> cameraListeners = new ArrayList<>();    
	
//	public Signal1<Vec3> getFocusChangedSignal() {
//		return focusChangedSignal;
//	}

	@Override
	public boolean incrementFocusPixels(double dx, double dy, double dz) {
		boolean result = markAndNotify(camera.incrementFocusPixels(dx, dy, dz));
		if (result) {
            fireFocusChanged(camera.getFocus());
        }
//			getFocusChangedSignal().emit(camera.getFocus());
		return result;
	}

	@Override
	public boolean incrementFocusPixels(Vec3 offset) {
		boolean result = markAndNotify(camera.incrementFocusPixels(offset));
		if (result) {
            fireFocusChanged(camera.getFocus());
        }
//			getFocusChangedSignal().emit(camera.getFocus());
		return result;
	}

	@Override
    public boolean incrementZoom(double zoomRatio) {
		boolean result = markAndNotify(camera.incrementZoom(zoomRatio));
		if (result) {
            fireZoomChanged(camera.getPixelsPerSceneUnit());
//			getZoomChangedSignal().emit(camera.getPixelsPerSceneUnit());
		}
		return result;
	}
	
	private boolean markAndNotify(boolean changed) {
		if (! changed)
			return false;
		// System.out.println("emit viewChanged");
        fireViewChanged();
//		getViewChangedSignal().emit();	
		return true;
	}

	@Override
	public Vec3 getFocus() {
		return camera.getFocus();
	}

	@Override
	public double getPixelsPerSceneUnit() {
		return camera.getPixelsPerSceneUnit();
	}

	@Override
	public Rotation3d getRotation() {
		return camera.getRotation();
	}

	@Override
	public boolean resetFocus() {
		boolean result = markAndNotify(camera.resetFocus());
		if (result) {
            fireFocusChanged(camera.getFocus());
        }
//			getFocusChangedSignal().emit(camera.getFocus());
		return result;
	}

	@Override
	public boolean resetRotation() {
		return markAndNotify(camera.resetRotation());
	}

	@Override
	public boolean setFocus(Vec3 f) {
		boolean result = markAndNotify(camera.setFocus(f));
		if (result) {
            fireFocusChanged(camera.getFocus());
        }
//			getFocusChangedSignal().emit(camera.getFocus());
		return result;
	}

	@Override
	public boolean setFocus(double x, double y, double z) {
		boolean result = markAndNotify(camera.setFocus(x, y, z));
		if (result) {
            fireFocusChanged(camera.getFocus());
        }
//			getFocusChangedSignal().emit(camera.getFocus());
		return result;		
	}

	@Override
	public boolean setRotation(Rotation3d r) {
		return markAndNotify(camera.setRotation(r));
	}

	@Override
	public boolean setPixelsPerSceneUnit(double pixelsPerSceneUnit) {
		boolean result = markAndNotify(camera.setPixelsPerSceneUnit(pixelsPerSceneUnit));
		if (result) {
            fireZoomChanged(camera.getPixelsPerSceneUnit());
//			getZoomChangedSignal().emit(camera.getPixelsPerSceneUnit());
		}
		return result;
	}

    //-------------------------------------IMPLEMENTS ObservableCamera3d
    @Override
    public void addCameraListener(CameraListener listener) {
        if (! cameraListeners.contains(listener))
            cameraListeners.add(listener);
    }
    
    @Override
    public void removeCameraListener(CameraListener listener) {
        cameraListeners.remove(listener);
    }

    @Override
    public void fireZoomChanged(Double newZoom) {
        for (CameraListener l: cameraListeners) {
            l.zoomChanged(newZoom);
        }
    }

    @Override
    public void fireFocusChanged(Vec3 newFocus) {
        for (CameraListener l: cameraListeners) {
            l.focusChanged(newFocus);
        }
    }

    @Override
    public void fireViewChanged() {
        for (CameraListener l: cameraListeners) {
            l.viewChanged();
        }
    }
}
