package org.janelia.it.workstation.gui.slice_viewer.action;

import javax.swing.KeyStroke;

import com.jogamp.newt.event.KeyEvent;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d;

public class NextZSliceAction
extends SliceScanAction
{
	private static final long serialVersionUID = 1L;

	public NextZSliceAction(VolumeImage3d image, Camera3d camera) {
		super(image, camera, 1);
		putValue(NAME, "Next Z Slice");
		putValue(SMALL_ICON, Icons.getIcon("z_stack_down.png"));
		putValue(MNEMONIC_KEY, (int)KeyEvent.VK_PAGE_DOWN);
		KeyStroke accelerator = KeyStroke.getKeyStroke(
			KeyEvent.VK_PAGE_DOWN, 0);
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(SHORT_DESCRIPTION,
				"View next Z slice"
				+"\n (Shortcut: "+accelerator+")"
				);		
	}
}
