package org.janelia.it.FlyWorkstation.gui.viewer3d.camera;

import java.util.Observable;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.QtSignal;

public class BasicObservableCamera3d 
extends Observable 
implements Camera3d, ObservableCamera3d
{
	private Camera3d camera = new BasicCamera3d();
	protected QtSignal<Object> viewChanged = new QtSignal<Object>();
	
	/* (non-Javadoc)
	 * @see org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d#getViewChangedSignal()
	 */
	@Override
	public QtSignal<Object> getViewChangedSignal() {
		return viewChanged;
	}

	@Override
	public boolean incrementFocusPixels(int dx, int dy, int dz) {
		return markAndNotify(camera.incrementFocusPixels(dx, dy, dz));
	}

	@Override
    public boolean incrementZoom(float zoomRatio) {
		return markAndNotify(camera.incrementZoom(zoomRatio));
	}
	
	private boolean markAndNotify(boolean changed) {
		if (! changed)
			return false;
		// System.out.println("emit viewChanged");
		viewChanged.emit(null);	
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
		return markAndNotify(camera.resetFocus());
	}

	@Override
	public boolean resetRotation() {
		return markAndNotify(camera.resetRotation());
	}

	@Override
	public boolean setFocus(Vec3 f) {
		return markAndNotify(camera.setFocus(f));
	}

	@Override
	public boolean setRotation(Rotation r) {
		return markAndNotify(camera.setRotation(r));
	}

	@Override
	public boolean setPixelsPerSceneUnit(double pixelsPerSceneUnit) {
		return markAndNotify(camera.setPixelsPerSceneUnit(pixelsPerSceneUnit));
	}

}
