package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.MouseWheelEvent;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.MouseModalWidget;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileFormat;

public class ZScanMode 
implements WheelMode
{
	BasicMouseMode mode = new BasicMouseMode();
	SliceScanAction sliceScanAction;

	public ZScanMode(VolumeImage3d image) {
		this.sliceScanAction = new SliceScanAction(image, mode.getCamera(), 1);
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

		sliceScanAction.setSliceCount(notches);
		sliceScanAction.actionPerformed(null);
	}

	@Override
	public void setCamera(Camera3d camera) {
		mode.setCamera(camera);
		sliceScanAction.setCamera(camera);
	}

	@Override
	public void setComponent(MouseModalWidget widget, boolean updateCursor) {
		mode.setComponent(widget, updateCursor);
	}
	
	public void setTileFormat(TileFormat tileFormat) {
		sliceScanAction.setTileFormat(tileFormat);
	}
	
	public void setSliceAxis(CoordinateAxis axis) {
	    sliceScanAction.setSliceAxis(axis);
	}
}
