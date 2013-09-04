package org.janelia.it.FlyWorkstation.gui.slice_viewer.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;

import com.jogamp.newt.event.KeyEvent;

public class ZoomOutAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	protected Camera3d camera;

	public ZoomOutAction(Camera3d camera) {
		this.camera = camera;
		putValue(NAME, "Zoom Out");
		putValue(SMALL_ICON, Icons.getIcon("magnify_minus.png"));
		putValue(MNEMONIC_KEY, KeyEvent.VK_MINUS); // works with keyboard
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
