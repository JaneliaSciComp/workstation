package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

public class AdvanceZSlicesAction extends ZScanAction {
	private static final long serialVersionUID = 1L;

	AdvanceZSlicesAction(VolumeImage3d image, Camera3d camera, int sliceCount) {
		super(image, camera, sliceCount);
		putValue(NAME, "Advance " + sliceCount + " Z Slices");
		putValue(SMALL_ICON, Icons.getIcon("z_stack_down_fast.png"));
		putValue(SHORT_DESCRIPTION,
				"Advance " + sliceCount + " Z slices");		
	}

}
