package org.janelia.it.FlyWorkstation.gui.slice_viewer.action;

import javax.swing.KeyStroke;

import org.janelia.it.FlyWorkstation.gui.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

import com.jogamp.newt.event.KeyEvent;

public class PreviousZSliceAction extends SliceScanAction {
	private static final long serialVersionUID = 1L;

	public PreviousZSliceAction(VolumeImage3d image, Camera3d camera) {
		super(image, camera, -1);
		putValue(NAME, "Previous Z Slice");
		putValue(SMALL_ICON, Icons.getIcon("z_stack_up.png"));
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
