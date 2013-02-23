package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

// PanModeAction puts the slice viewer into Pan mode.
public class ZScanScrollModeAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	protected MouseModalWidget widget;
	protected VolumeImage3d image;

	public ZScanScrollModeAction(MouseModalWidget widget, VolumeImage3d image) {
		putValue(NAME, "Z-Scan");
		putValue(SMALL_ICON, Icons.getIcon("z_stack.png"));
		putValue(SHORT_DESCRIPTION, 
				"Set scroll wheel mode to reveal different image slices.");
		this.widget = widget;
		this.image = image;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		WheelMode mode = new ZScanMode(image);
		mode.setComponent(widget);
		widget.setWheelMode(mode);
	}
}
