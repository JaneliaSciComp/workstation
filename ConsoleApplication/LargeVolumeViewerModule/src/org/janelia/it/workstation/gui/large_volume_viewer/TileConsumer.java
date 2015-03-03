package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.viewer3d.interfaces.Viewport;

public interface TileConsumer {
	Camera3d getCamera();
	Viewport getViewport();
	CoordinateAxis getSliceAxis();
	Rotation3d getViewerInGround();
	boolean isShowing();
    void repaint();
}
