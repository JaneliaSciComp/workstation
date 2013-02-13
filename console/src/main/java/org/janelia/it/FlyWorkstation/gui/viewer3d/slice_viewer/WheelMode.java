package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.event.MouseWheelListener;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.Camera3d;

public interface WheelMode
extends MouseWheelListener
{
	public MouseModalWidget getComponent();
	public void setComponent(MouseModalWidget widget);
	Camera3d getCamera();
	void setCamera(Camera3d camera);
}
