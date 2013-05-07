package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

public class ZoomMaxAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;

	protected Camera3d camera;
	protected VolumeImage3d image;

	public ZoomMaxAction(Camera3d camera, VolumeImage3d image) {
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
