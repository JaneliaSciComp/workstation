package org.janelia.it.FlyWorkstation.gui.slice_viewer.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.signal.Signal1;

// PanModeAction puts the slice viewer into Pan mode.
public class PanModeAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	
	public Signal1<MouseMode.Mode> setMouseModeSignal = new Signal1<MouseMode.Mode>();

	public PanModeAction() {
		putValue(NAME, "Pan");
		putValue(SMALL_ICON, Icons.getIcon("grab_opened.png"));
		String acc = "H";
		KeyStroke accelerator = KeyStroke.getKeyStroke(acc);
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(SHORT_DESCRIPTION, 
				"<html>"
				+"Set mouse mode to Pan left right up or down.["+acc+"]<br>"
				+"<br>"
				+"Click and drag to move image<br>"
				+"Scroll wheel to scan Z<br>" // done
				+"SHIFT-scroll wheel to zoom<br>" // done
				+"Double-click to recenter on a point<br>"
				+"Right-click for context menu"
				+"</html>");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) 
	{
		setMouseModeSignal.emit(MouseMode.Mode.PAN);
		putValue(SELECTED_KEY, true);
	}
}
