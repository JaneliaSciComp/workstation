package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Rotation3d;
import org.janelia.it.FlyWorkstation.gui.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.signal.Slot;

public interface TileConsumer {
	Camera3d getCamera();
	Viewport getViewport();
	CoordinateAxis getSliceAxis();
	Rotation3d getViewerInGround();
	boolean isShowing();
	Slot getRepaintSlot();
}
