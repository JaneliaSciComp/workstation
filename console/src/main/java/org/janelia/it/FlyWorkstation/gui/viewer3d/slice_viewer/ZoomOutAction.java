package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;

public class ZoomOutAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	protected Camera3d camera;

	public ZoomOutAction(Camera3d camera) {
		this.camera = camera;
		putValue(NAME, "Zoom Out");
		putValue(SMALL_ICON, Icons.getIcon("magnify_minus.png"));
		putValue(SHORT_DESCRIPTION,
				"Zoom out to shrunken image.");
	}
	
	@Override
	public void actionPerformed(ActionEvent event) 
	{
		camera.incrementZoom(1.0/1.414);
	}

}
