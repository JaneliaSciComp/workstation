package org.janelia.it.workstation.gui.slice_viewer.action;

import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.signal.Signal1;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

// PanModeAction puts the slice viewer into Pan mode.
public class ZoomMouseModeAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	
    public Signal1<MouseMode.Mode> setMouseModeSignal = new Signal1<MouseMode.Mode>();

	public ZoomMouseModeAction() {
		putValue(NAME, "Zoom");
		putValue(SMALL_ICON, Icons.getIcon("magnify_glass.png"));
		String acc = "Z";
		KeyStroke accelerator = KeyStroke.getKeyStroke(acc);
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(SHORT_DESCRIPTION, 
				"Set mouse mode to Zoom in and out."
				+ "\n (Shortcut: " + acc + ")");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		setMouseModeSignal.emit(MouseMode.Mode.ZOOM);
		putValue(SELECTED_KEY, true);
	}
}
