package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.janelia.it.FlyWorkstation.gui.util.Icons;

// PanModeAction puts the slice viewer into Pan mode.
public class ZoomScrollModeAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	protected MouseModalWidget widget;

	public ZoomScrollModeAction(MouseModalWidget widget) {
		putValue(NAME, "Zoom");
		putValue(SMALL_ICON, Icons.getIcon("magnify_glass.png"));
		putValue(SHORT_DESCRIPTION, 
				"Set scroll wheel mode to Zoom in and out.");
		this.widget = widget;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		WheelMode mode = new ZoomMode();
		mode.setComponent(widget);
		widget.setWheelMode(mode);
		putValue(SELECTED_KEY, true);
	}
}
