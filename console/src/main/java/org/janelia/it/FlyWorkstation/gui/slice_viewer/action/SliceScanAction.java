package org.janelia.it.FlyWorkstation.gui.slice_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileFormat;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileIndex;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

/**
 * Base class for z slice scanning actions in slice viewer.
 * 
 * @author Christopher M. Bruns
 *
 */
public class SliceScanAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	private VolumeImage3d image;
	private Camera3d camera;
	private int sliceCount;
	private TileFormat tileFormat;
	private CoordinateAxis sliceAxis = CoordinateAxis.Z;
	
	SliceScanAction(VolumeImage3d image, Camera3d camera, int sliceCount)
	{
		this.image = image;
		this.camera = camera;
		this.sliceCount = sliceCount;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) 
	{
	    int axisIx = sliceAxis.index();
		Vec3 oldFocus = camera.getFocus();
		double oldVal = oldFocus.get(axisIx);
		int oldSliceIndex = (int)(Math.round(oldVal / image.getResolution(axisIx)) - 0.5);

		// Take larger steps at lower octree zoom levels
		int zoomedSliceCount = sliceCount;
		if ((tileFormat != null) && (tileFormat.getIndexStyle() == TileIndex.IndexStyle.OCTREE))
		{
			int zoom = tileFormat.zoomLevelForCameraZoom(camera.getPixelsPerSceneUnit());
			int deltaSlice = (int)Math.pow(2, zoom);
			zoomedSliceCount = sliceCount * deltaSlice;
		}
		
		int newSliceIndex = oldSliceIndex + zoomedSliceCount;
		
		// Scoot to next multiple of 10, for (apparent) performance
		int sliceIncrement = Math.abs(sliceCount);
		if (sliceIncrement == 10) {
			int dSlice = newSliceIndex % sliceIncrement;
			if (dSlice > sliceIncrement/2.0)
				dSlice -= sliceIncrement;
			newSliceIndex -= dSlice; // newZIndex is now a multiple of sliceCount
			assert newSliceIndex % sliceIncrement == 0;
		}

		double halfVoxel = 0.5 * image.getResolution(axisIx);
		double newSlice = newSliceIndex * image.getResolution(axisIx) + halfVoxel;
		double maxSlice = image.getBoundingBox3d().getMax().get(axisIx) - halfVoxel;
		double minSlice = image.getBoundingBox3d().getMin().get(axisIx) + halfVoxel;
		assert maxSlice >= minSlice;
		if (newSlice > maxSlice)
			newSlice = maxSlice;
		if (newSlice < minSlice)
			newSlice = minSlice;
		if (newSlice == oldVal)
			return; // no change
		Vec3 newFocus = new Vec3(oldFocus.getX(), oldFocus.getY(), oldFocus.getZ());
		newFocus.set(axisIx, newSlice);
		// System.out.println(newFocus.get(axisIx)+", "+newSliceIndex);
		// TODO - disallow camera.getFocus().setZ(), which bypasses camera signaling
		camera.setFocus(newFocus);
	}

	public CoordinateAxis getSliceAxis() {
        return sliceAxis;
    }

    public void setSliceAxis(CoordinateAxis sliceAxis) {
        this.sliceAxis = sliceAxis;
    }

    public int getSliceCount() {
		return sliceCount;
	}

	public TileFormat getTileFormat() {
		return tileFormat;
	}

	public void setSliceCount(int sliceCount) {
		this.sliceCount = sliceCount;
	}

	public void setTileFormat(TileFormat tileFormat) {
		this.tileFormat = tileFormat;
	}

	public Camera3d getCamera() {
		return camera;
	}

	public void setCamera(Camera3d camera) {
		this.camera = camera;
	}

}
