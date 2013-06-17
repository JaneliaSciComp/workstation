package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import javax.swing.KeyStroke;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

import com.jogamp.newt.event.KeyEvent;

public class GoBackZSlicesAction extends SliceScanAction {
	private static final long serialVersionUID = 1L;

	public GoBackZSlicesAction(VolumeImage3d image, Camera3d camera, int sliceCount) {
		super(image, camera, sliceCount);
		putValue(NAME, "Go Back " + -sliceCount + " Z Slices");
		putValue(SMALL_ICON, Icons.getIcon("z_stack_up_fast.png"));
		putValue(MNEMONIC_KEY, KeyEvent.VK_LESS);
		// Shift-comma, really!??! Qt is better than Java. Period.
		KeyStroke accelerator = KeyStroke.getKeyStroke(
			KeyEvent.VK_COMMA, 
			KeyEvent.SHIFT_MASK);
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(SHORT_DESCRIPTION,
				"Go back " + -sliceCount + " Z slices"
				+"\n (Shortcut: "+accelerator+")"
				);		
	}

}
