package org.janelia.it.workstation.gui.camera;

import java.util.Observable;

import org.janelia.it.workstation.geom.Vec3;

public class BasicObservableCamera3d 
extends Observable 
implements Camera3d, ObservableCamera3d
{
	private Camera3d camera = new BasicCamera3d();
	protected org.janelia.it.workstation.signal.Signal viewChangedSignal = new org.janelia.it.workstation.signal.Signal();
	protected org.janelia.it.workstation.signal.Signal1<Double> zoomChangedSignal = new org.janelia.it.workstation.signal.Signal1<Double>();
	protected org.janelia.it.workstation.signal.Signal1<Vec3> focusChangedSignal = new org.janelia.it.workstation.signal.Signal1<Vec3>();

	
	public org.janelia.it.workstation.signal.Signal1<Vec3> getFocusChangedSignal() {
		return focusChangedSignal;
	}

	/* (non-Javadoc)
	 * @see org.janelia.it.workstation.gui.viewer3d.camera.ObservableCamera3d#getViewChangedSignal()
	 */
	@Override
	public org.janelia.it.workstation.signal.Signal getViewChangedSignal() {
		return viewChangedSignal;
	}
	
	@Override
	public org.janelia.it.workstation.signal.Signal1<Double> getZoomChangedSignal() {
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
	public boolean incrementFocusPixels(org.janelia.it.workstation.geom.Vec3 offset) {
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
	public org.janelia.it.workstation.geom.Vec3 getFocus() {
		return camera.getFocus();
	}

	@Override
	public double getPixelsPerSceneUnit() {
		return camera.getPixelsPerSceneUnit();
	}

	@Override
	public org.janelia.it.workstation.geom.Rotation3d getRotation() {
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
	public boolean setFocus(org.janelia.it.workstation.geom.Vec3 f) {
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
	public boolean setRotation(org.janelia.it.workstation.geom.Rotation3d r) {
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
