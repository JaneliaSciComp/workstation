package org.janelia.it.FlyWorkstation.gui.slice_viewer.action;

import java.awt.event.InputEvent;

import javax.swing.KeyStroke;

import org.janelia.it.FlyWorkstation.gui.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

import com.jogamp.newt.event.KeyEvent;

public class AdvanceZSlicesAction extends SliceScanAction {
	private static final long serialVersionUID = 1L;

	public AdvanceZSlicesAction(VolumeImage3d image, Camera3d camera, int sliceCount) {
		super(image, camera, sliceCount);
		putValue(NAME, "Advance " + sliceCount + " Z Slices");
		putValue(SMALL_ICON, Icons.getIcon("z_stack_down_fast.png"));
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
