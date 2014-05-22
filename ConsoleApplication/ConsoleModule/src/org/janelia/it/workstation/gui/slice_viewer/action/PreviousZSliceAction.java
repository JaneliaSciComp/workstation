package org.janelia.it.workstation.gui.slice_viewer.action;

import javax.swing.KeyStroke;

import com.jogamp.newt.event.KeyEvent;

public class PreviousZSliceAction extends SliceScanAction {
	private static final long serialVersionUID = 1L;

	public PreviousZSliceAction(org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d image, org.janelia.it.workstation.gui.camera.Camera3d camera) {
		super(image, camera, -1);
		putValue(NAME, "Previous Z Slice");
		putValue(SMALL_ICON, org.janelia.it.workstation.gui.util.Icons.getIcon("z_stack_up.png"));
		putValue(MNEMONIC_KEY, (int)KeyEvent.VK_PAGE_UP);
		KeyStroke accelerator = KeyStroke.getKeyStroke(
			KeyEvent.VK_PAGE_UP, 0);
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(SHORT_DESCRIPTION,
				"View previous Z slice"
				+"\n (Shortcut: "+accelerator+")"
				);		
	}

}
