package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

public class ResetColorsAction 
extends AbstractAction 
implements Action 
{
	private static final long serialVersionUID = 1L;
	
	private ImageColorModel imageColorModel;

	public ResetColorsAction(ImageColorModel imageColorModel) {
		this.imageColorModel = imageColorModel;
		putValue(NAME, "Reset Colors");
		putValue(SHORT_DESCRIPTION,
				"Restore colors and brightness to default values");
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		imageColorModel.resetColors();
	}

}
