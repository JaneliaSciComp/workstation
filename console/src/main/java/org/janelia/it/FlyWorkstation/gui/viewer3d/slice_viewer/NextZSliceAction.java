package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

public class NextZSliceAction
extends ZScanAction
{
	private static final long serialVersionUID = 1L;

	NextZSliceAction(VolumeImage3d image, Camera3d camera) {
		super(image, camera, 1);
		putValue(NAME, "Next Z Slice");
		putValue(SMALL_ICON, Icons.getIcon("z_stack_down.png"));
		putValue(SHORT_DESCRIPTION,
				"View next Z slice");		
	}
}
