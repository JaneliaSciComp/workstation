package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

public class UndoAction extends AbstractAction
{
	private static final long serialVersionUID = 1L;

	public UndoAction() {
		putValue(NAME, "Undo");
		// control-Z or command-Z (on Mac)
		KeyStroke accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_Z, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		putValue(ACCELERATOR_KEY, accelerator);
		setEnabled(false);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("Undo");  // TODO
	}
}
