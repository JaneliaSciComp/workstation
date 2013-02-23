package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.event.MouseWheelEvent;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

public class ZScanMode 
implements WheelMode
{
	BasicMouseMode mode = new BasicMouseMode();
	private VolumeImage3d image;

	public ZScanMode(VolumeImage3d image) {
		this.image = image;
	}
	
	@Override
	public MouseModalWidget getComponent() {
		return mode.getComponent();
	}

	@Override
	public Camera3d getCamera() {
		return mode.getCamera();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent event) 
	{
		Camera3d camera = getCamera();
		if (camera == null)
			return;
		int notches = event.getWheelRotation();
		if (notches == 0)
			return;

		Vec3 oldFocus = camera.getFocus();
		double oldZ = oldFocus.getZ();
		int oldZIndex = (int)(Math.round(oldZ / image.getZResolution()) + 0.1);
		int sliceCount = notches;
		int newZIndex = oldZIndex + sliceCount;
		double newZ = newZIndex * image.getZResolution();
		double maxZ = image.getBoundingBox3d().getMax().getZ();
		double minZ = image.getBoundingBox3d().getMin().getZ();
		assert maxZ >= minZ;
		if (newZ > maxZ)
			newZ = maxZ;
		if (newZ < minZ)
			newZ = minZ;
		if (newZ == oldZ)
			return; // no change
		Vec3 newFocus = new Vec3(oldFocus.getX(), oldFocus.getY(), newZ);
		// TODO - disallow camera.getFocus().setZ(), which bypasses camera signaling
		camera.setFocus(newFocus);
	}

	@Override
	public void setCamera(Camera3d camera) {
		mode.setCamera(camera);
	}

	@Override
	public void setComponent(MouseModalWidget widget) {
		mode.setComponent(widget);
	}
}
