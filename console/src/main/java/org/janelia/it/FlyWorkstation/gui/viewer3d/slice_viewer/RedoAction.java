package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

// PanModeAction puts the slice viewer into Pan mode.
public class RedoAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;

	public RedoAction() {
		putValue(NAME, "Redo");
		// control-Y windows or command-shift-Z (on Mac)
		// TODO - command-shift z on Mac
		KeyStroke accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_Y, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		putValue(ACCELERATOR_KEY, accelerator);
		setEnabled(false);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("Redo");  // TODO
	}
}
