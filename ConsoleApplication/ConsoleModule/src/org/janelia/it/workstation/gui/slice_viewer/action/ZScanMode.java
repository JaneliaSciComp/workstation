package org.janelia.it.workstation.gui.slice_viewer.action;

import java.awt.event.MouseWheelEvent;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.gui.slice_viewer.MouseModalWidget;

public class ZScanMode 
implements WheelMode
{
	org.janelia.it.workstation.gui.slice_viewer.action.BasicMouseMode mode = new org.janelia.it.workstation.gui.slice_viewer.action.BasicMouseMode();
	org.janelia.it.workstation.gui.slice_viewer.action.SliceScanAction sliceScanAction;

	public ZScanMode(org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d image) {
		this.sliceScanAction = new org.janelia.it.workstation.gui.slice_viewer.action.SliceScanAction(image, mode.getCamera(), 1);
	}
	
	@Override
	public MouseModalWidget getWidget() {
		return mode.getWidget();
	}

	@Override
	public org.janelia.it.workstation.gui.camera.Camera3d getCamera() {
		return mode.getCamera();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent event) 
	{
		org.janelia.it.workstation.gui.camera.Camera3d camera = getCamera();
		if (camera == null)
			return;
		int notches = event.getWheelRotation();
		if (notches == 0)
			return;
		
		int deltaZ = 1;

		sliceScanAction.setSliceCount(notches*deltaZ);
		sliceScanAction.actionPerformed(null);
	}

	@Override
	public void setCamera(org.janelia.it.workstation.gui.camera.Camera3d camera) {
		mode.setCamera(camera);
		sliceScanAction.setCamera(camera);
	}

	@Override
	public void setWidget(MouseModalWidget widget, boolean updateCursor) {
		mode.setWidget(widget, updateCursor);
	}
	
	public void setTileFormat(org.janelia.it.workstation.gui.slice_viewer.TileFormat tileFormat) {
		sliceScanAction.setTileFormat(tileFormat);
	}
	
	public void setSliceAxis(CoordinateAxis axis) {
	    sliceScanAction.setSliceAxis(axis);
	}
}
