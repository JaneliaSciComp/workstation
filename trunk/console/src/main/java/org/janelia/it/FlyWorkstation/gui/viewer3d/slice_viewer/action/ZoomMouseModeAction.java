package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.MouseModalWidget;

// PanModeAction puts the slice viewer into Pan mode.
public class ZoomMouseModeAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	protected MouseModalWidget widget;

	public ZoomMouseModeAction(MouseModalWidget widget) {
		putValue(NAME, "Zoom");
		putValue(SMALL_ICON, Icons.getIcon("magnify_glass.png"));
		String acc = "Z";
		KeyStroke accelerator = KeyStroke.getKeyStroke(acc);
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(SHORT_DESCRIPTION, 
				"Set mouse mode to Zoom in and out."
				+ "\n (Shortcut: " + acc + ")");
		this.widget = widget;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		MouseMode mode = new ZoomMode();
		mode.setComponent(widget);
		widget.setMouseMode(mode);
		putValue(SELECTED_KEY, true);
	}
}
