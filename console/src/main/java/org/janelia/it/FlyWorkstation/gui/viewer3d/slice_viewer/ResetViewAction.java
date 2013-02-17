package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.janelia.it.FlyWorkstation.gui.util.Icons;

public class ResetViewAction 
extends AbstractAction
{
	private static final long serialVersionUID = 1L;
	protected SliceViewer widget; // TODO interface here

	public ResetViewAction(SliceViewer widget) {
		this.widget = widget;
		putValue(NAME, "Reset View");
		putValue(SHORT_DESCRIPTION,
				"Recenter and show entire volume.");
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		widget.resetView();
	}
}
