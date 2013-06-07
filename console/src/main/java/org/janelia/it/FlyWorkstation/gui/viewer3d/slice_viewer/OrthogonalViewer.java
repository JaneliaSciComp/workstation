package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

/**
 * Intended replacement class for SliceViewer,
 * generalized for X,Y,Z orthogonal views
 * @author brunsc
 *
 */
public class OrthogonalViewer 
{
	private Camera3d camera;
	private Viewport viewport;
	private VolumeImage3d volume;
	private CoordinateAxis viewAxis;
	
	public OrthogonalViewer(CoordinateAxis axis) {
		this.viewAxis = axis;
	}
	
	public void setCamera(ObservableCamera3d camera) {
		this.camera = camera;
	}
	
	public void setVolumeImage3d(VolumeImage3d volume) {
		this.volume = volume;
	}
}
