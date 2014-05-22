package org.janelia.it.workstation.gui.slice_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class ZoomMaxAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;

	protected org.janelia.it.workstation.gui.camera.Camera3d camera;
	protected org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d image;

	public ZoomMaxAction(org.janelia.it.workstation.gui.camera.Camera3d camera, org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d image) {
		this.camera = camera;
		this.image = image;
		putValue(NAME, "Zoom Max");
		putValue(SHORT_DESCRIPTION,
				"Zoom in to full resolution.");
	}
	
	@Override
	public void actionPerformed(ActionEvent event) 
	{
		double maxRes = Math.min(image.getXResolution(), image.getYResolution());
		camera.setPixelsPerSceneUnit(1.0/maxRes);
	}

}
