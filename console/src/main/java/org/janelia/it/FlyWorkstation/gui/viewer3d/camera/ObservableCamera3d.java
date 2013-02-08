package org.janelia.it.FlyWorkstation.gui.viewer3d.camera;

import java.util.Observable;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;


public class ObservableCamera3d 
extends Observable 
implements Camera3d
{
	private Camera3d camera = new SimpleCamera3d();
	
	@Override
    public boolean incrementZoom(float zoomRatio) {
		return markAndNotify(camera.incrementZoom(zoomRatio));
	}
	
	private boolean markAndNotify(boolean changed) {
		if (! changed)
			return false;
		setChanged();
		notifyObservers();
		clearChanged();		
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
