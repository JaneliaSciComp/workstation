package org.janelia.it.workstation.gui.slice_viewer.action;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import org.janelia.it.workstation.gui.slice_viewer.TileConsumer;

public class ResetViewAction 
extends AbstractAction
{
	private static final long serialVersionUID = 1L;
	protected List<TileConsumer> widgets;
	private org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d volumeImage;
	private ResetZoomAction resetZoomAction;

	public ResetViewAction(
			List<TileConsumer> widgets,
			org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d volumeImage)
	{
		this.widgets = widgets;
		this.volumeImage = volumeImage;
		resetZoomAction = new ResetZoomAction(widgets, volumeImage);
		putValue(NAME, "Reset View");
		putValue(SHORT_DESCRIPTION,
				"Recenter and show entire volume.");
	}
	
	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		if (widgets.size() < 1)
			return;
		// First center...
		org.janelia.it.workstation.gui.camera.Camera3d camera = widgets.iterator().next().getCamera();
		if (! camera.setFocus(volumeImage.getVoxelCenter()))
			return;
		// ...then scale.
		resetZoomAction.actionPerformed(actionEvent);
	}
}
