package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.janelia.it.FlyWorkstation.gui.util.Icons;

// PanModeAction puts the slice viewer into Pan mode.
public class PanModeAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	
	// TODO generalize to use some Interface with setMouseMode() method,
	// instead of SliceViewer
	protected SliceViewer widget;

	public PanModeAction(SliceViewer widget) {
		putValue(NAME, "Pan");
		putValue(SMALL_ICON, Icons.getIcon("grab_opened.png"));
		String acc = "H";
		KeyStroke accelerator = KeyStroke.getKeyStroke(acc);
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(SHORT_DESCRIPTION, 
				"Set mouse mode to Pan left right up or down."
				+ "\nShortcut: " + acc);
		this.widget = widget;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		widget.setPanMode();
	}
}
