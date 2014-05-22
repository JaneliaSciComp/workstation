package org.janelia.it.workstation.gui.slice_viewer.action;

import javax.swing.KeyStroke;

import com.jogamp.newt.event.KeyEvent;

public class AdvanceZSlicesAction extends SliceScanAction {
	private static final long serialVersionUID = 1L;

	public AdvanceZSlicesAction(org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d image, org.janelia.it.workstation.gui.camera.Camera3d camera, int sliceCount) {
		super(image, camera, sliceCount);
		putValue(NAME, "Advance " + sliceCount + " Z Slices");
		putValue(SMALL_ICON, org.janelia.it.workstation.gui.util.Icons.getIcon("z_stack_down_fast.png"));
		putValue(MNEMONIC_KEY, (int)KeyEvent.VK_GREATER);
		KeyStroke accelerator = KeyStroke.getKeyStroke(
			KeyEvent.VK_PERIOD, 
			KeyEvent.SHIFT_MASK);
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(SHORT_DESCRIPTION,
				"Advance " + sliceCount + " Z slices"
				+"\n (Shortcut: "+accelerator+")"
				);
	}

}
