package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

// PanModeAction puts the slice viewer into Pan mode.
public class ZScanScrollModeAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	WheelMode zScanMode;
	protected MouseModalWidget widget;

	public ZScanScrollModeAction(MouseModalWidget widget, VolumeImage3d image) {
		putValue(NAME, "Z-Scan");
		putValue(SMALL_ICON, Icons.getIcon("z_stack.png"));
		putValue(SHORT_DESCRIPTION, 
				"Set scroll wheel mode to reveal different image slices.");
		this.widget = widget;
		zScanMode = new ZScanMode(image);
		zScanMode.setComponent(widget);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		widget.setWheelMode(zScanMode);
		putValue(SELECTED_KEY, true);
	}
}
