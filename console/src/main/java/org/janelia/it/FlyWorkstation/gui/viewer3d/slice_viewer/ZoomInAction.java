package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.Camera3d;

public class ZoomInAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	protected Camera3d camera;

	public ZoomInAction(Camera3d camera) {
		this.camera = camera;
		putValue(NAME, "Zoom In");
		putValue(SMALL_ICON, Icons.getIcon("magnify_plus.png"));
		putValue(SHORT_DESCRIPTION,
				"Zoom in to magnified image.");
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		camera.incrementZoom(1.414);
	}

}
