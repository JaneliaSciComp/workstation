package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.MouseWheelEvent;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.MouseModalWidget;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileFormat;

public class ZScanMode 
implements WheelMode
{
	BasicMouseMode mode = new BasicMouseMode();
	ZScanAction zScanAction;

	public ZScanMode(VolumeImage3d image) {
		this.zScanAction = new ZScanAction(image, mode.getCamera(), 1);
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

		zScanAction.setSliceCount(notches);
		zScanAction.actionPerformed(null);
	}

	@Override
	public void setCamera(Camera3d camera) {
		mode.setCamera(camera);
		zScanAction.setCamera(camera);
	}

	@Override
	public void setComponent(MouseModalWidget widget) {
		mode.setComponent(widget);
	}
	
	public void setTileFormat(TileFormat tileFormat) {
		zScanAction.setTileFormat(tileFormat);
	}
}
