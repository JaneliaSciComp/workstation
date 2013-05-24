package org.janelia.it.FlyWorkstation.gui.viewer3d.camera;

import java.util.Observable;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal1;

public class BasicObservableCamera3d 
extends Observable 
implements Camera3d, ObservableCamera3d
{
	private Camera3d camera = new BasicCamera3d();
	protected Signal viewChangedSignal = new Signal();
	protected Signal1<Double> zoomChangedSignal = new Signal1<Double>();
	protected Signal1<Vec3> focusChangedSignal = new Signal1<Vec3>();

	
	public Signal1<Vec3> getFocusChangedSignal() {
		return focusChangedSignal;
	}

	/* (non-Javadoc)
	 * @see org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d#getViewChangedSignal()
	 */
	@Override
	public Signal getViewChangedSignal() {
		return viewChangedSignal;
	}
	
	@Override
	public Signal1<Double> getZoomChangedSignal() {
		return zoomChangedSignal;
	}

	@Override
	public boolean incrementFocusPixels(double dx, double dy, double dz) {
		boolean result = markAndNotify(camera.incrementFocusPixels(dx, dy, dz));
		if (result)
			getFocusChangedSignal().emit(camera.getFocus());
		return result;
	}

	@Override
	public boolean incrementFocusPixels(Vec3 offset) {
		boolean result = markAndNotify(camera.incrementFocusPixels(offset));
		if (result)
			getFocusChangedSignal().emit(camera.getFocus());
		return result;
	}

	@Override
    public boolean incrementZoom(double zoomRatio) {
		boolean result = markAndNotify(camera.incrementZoom(zoomRatio));
		if (result) {
			getZoomChangedSignal().emit(camera.getPixelsPerSceneUnit());
		}
		return result;
	}
	
	private boolean markAndNotify(boolean changed) {
		if (! changed)
			return false;
		// System.out.println("emit viewChanged");
		getViewChangedSignal().emit();	
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
	public Rotation getRotation() {
		return camera.getRotation();
	}

	@Override
	public boolean resetFocus() {
		boolean result = markAndNotify(camera.resetFocus());
		if (result)
			getFocusChangedSignal().emit(camera.getFocus());
		return result;
	}

	@Override
	public boolean resetRotation() {
		return markAndNotify(camera.resetRotation());
	}

	@Override
	public boolean setFocus(Vec3 f) {
		boolean result = markAndNotify(camera.setFocus(f));
		if (result)
			getFocusChangedSignal().emit(camera.getFocus());
		return result;
	}

	@Override
	public boolean setFocus(double x, double y, double z) {
		boolean result = markAndNotify(camera.setFocus(x, y, z));
		if (result)
			getFocusChangedSignal().emit(camera.getFocus());
		return result;		
	}

	@Override
	public boolean setRotation(Rotation r) {
		return markAndNotify(camera.setRotation(r));
	}

	@Override
	public boolean setPixelsPerSceneUnit(double pixelsPerSceneUnit) {
		boolean result = markAndNotify(camera.setPixelsPerSceneUnit(pixelsPerSceneUnit));
		if (result) {
			getZoomChangedSignal().emit(camera.getPixelsPerSceneUnit());
		}
		return result;
	}
}
