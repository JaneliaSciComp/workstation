package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import com.jogamp.newt.event.KeyEvent;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.util.Icons;

public class ZoomOutAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	protected Camera3d camera;

	public ZoomOutAction(Camera3d camera) {
		this.camera = camera;
		putValue(NAME, "Zoom Out");
		putValue(SMALL_ICON, Icons.getIcon("magnify_minus.png"));
		putValue(MNEMONIC_KEY, (int)KeyEvent.VK_MINUS); // works with keyboard
		// ctrl-minus on windows, cmd-minus on Mac
		KeyStroke accelerator = KeyStroke.getKeyStroke(
				KeyEvent.VK_MINUS,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(SHORT_DESCRIPTION,
				"Zoom out to shrunken image."
				+"\n (Shortcut: "+accelerator+")"
				);
	}
	
	@Override
	public void actionPerformed(ActionEvent event) 
	{
		camera.incrementZoom(1.0/1.414);
	}

}
