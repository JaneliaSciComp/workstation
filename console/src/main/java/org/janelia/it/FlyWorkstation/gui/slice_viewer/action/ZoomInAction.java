package org.janelia.it.FlyWorkstation.gui.slice_viewer.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.janelia.it.FlyWorkstation.gui.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.util.Icons;

import com.jogamp.newt.event.KeyEvent;

public class ZoomInAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	protected Camera3d camera;

	public ZoomInAction(Camera3d camera) {
		this.camera = camera;
		putValue(NAME, "Zoom In");
		putValue(SMALL_ICON, Icons.getIcon("magnify_plus.png"));
		putValue(MNEMONIC_KEY, (int)KeyEvent.VK_PLUS);
		// ctrl-plus on windows, cmd-plus on Mac
		KeyStroke accelerator = KeyStroke.getKeyStroke(
				KeyEvent.VK_EQUALS,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(SHORT_DESCRIPTION,
				"Zoom in to magnified image."
				+"\n (Shortcut: "+accelerator+")"
				);
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		camera.incrementZoom(1.414);
	}

}
