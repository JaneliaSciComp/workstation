package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class ResetZoomAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
	protected SliceViewer widget; // TODO interface here

	public ResetZoomAction(SliceViewer widget) {
		this.widget = widget;
		putValue(NAME, "Zoom Min");
		putValue(SHORT_DESCRIPTION,
				"Zoom out to show entire volume.");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		widget.resetZoom();
	}

}
