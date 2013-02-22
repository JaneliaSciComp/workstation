package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

public class GoBackZSlicesAction extends ZScanAction {
	private static final long serialVersionUID = 1L;

	GoBackZSlicesAction(VolumeImage3d image, Camera3d camera, int sliceCount) {
		super(image, camera, sliceCount);
		putValue(NAME, "Go Back " + -sliceCount + " Z Slices");
		putValue(SMALL_ICON, Icons.getIcon("z_stack_up_fast.png"));
		putValue(SHORT_DESCRIPTION,
				"Go back " + -sliceCount + " Z slices");		
	}

}
