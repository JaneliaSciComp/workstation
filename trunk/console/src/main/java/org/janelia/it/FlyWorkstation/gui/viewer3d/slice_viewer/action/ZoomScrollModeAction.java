package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.MouseModalWidget;

// PanModeAction puts the slice viewer into Pan mode.
public class ZoomScrollModeAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	protected MouseModalWidget widget;
	protected ZoomMode zoomMode = new ZoomMode();

	public ZoomScrollModeAction(MouseModalWidget widget) {
		putValue(NAME, "Zoom");
		putValue(SMALL_ICON, Icons.getIcon("magnify_glass.png"));
		putValue(SHORT_DESCRIPTION, 
				"Set scroll wheel mode to Zoom in and out."
				+ "\n (hold SHIFT key to activate)");
		this.widget = widget;
		zoomMode.setComponent(widget);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		widget.setWheelMode(zoomMode);
		putValue(SELECTED_KEY, true); // this mode is now selected
	}
}
