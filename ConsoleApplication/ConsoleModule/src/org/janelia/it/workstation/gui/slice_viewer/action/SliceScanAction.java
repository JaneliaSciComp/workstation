package org.janelia.it.workstation.gui.slice_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Base class for z slice scanning actions in slice viewer.
 * 
 * @author Christopher M. Bruns
 *
 */
public class SliceScanAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	private org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d image;
	private org.janelia.it.workstation.gui.camera.Camera3d camera;
	private int sliceCount;
	private org.janelia.it.workstation.gui.slice_viewer.TileFormat tileFormat;
	private org.janelia.it.workstation.geom.CoordinateAxis sliceAxis = org.janelia.it.workstation.geom.CoordinateAxis.Z;
	
	SliceScanAction(org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d image, org.janelia.it.workstation.gui.camera.Camera3d camera, int sliceCount)
	{
		this.image = image;
		this.camera = camera;
		this.sliceCount = sliceCount;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) 
	{
	    int axisIx = sliceAxis.index();
		org.janelia.it.workstation.geom.Vec3 oldFocus = camera.getFocus();
		double oldVal = oldFocus.get(axisIx);
		int oldSliceIndex = (int)(Math.round(oldVal / image.getResolution(axisIx)) - 0.5);

		// Take larger steps at lower octree zoom levels
		int zoomedSliceCount = sliceCount;
		if ((tileFormat != null) && (tileFormat.getIndexStyle() == org.janelia.it.workstation.gui.slice_viewer.TileIndex.IndexStyle.OCTREE))
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
		org.janelia.it.workstation.geom.Vec3 newFocus = new org.janelia.it.workstation.geom.Vec3(oldFocus.getX(), oldFocus.getY(), oldFocus.getZ());
		newFocus.set(axisIx, newSlice);
		// System.out.println(newFocus.get(axisIx)+", "+newSliceIndex);
		// TODO - disallow camera.getFocus().setZ(), which bypasses camera signaling
		camera.setFocus(newFocus);
	}

	public org.janelia.it.workstation.geom.CoordinateAxis getSliceAxis() {
        return sliceAxis;
    }

    public void setSliceAxis(org.janelia.it.workstation.geom.CoordinateAxis sliceAxis) {
        this.sliceAxis = sliceAxis;
    }

    public int getSliceCount() {
		return sliceCount;
	}

	public org.janelia.it.workstation.gui.slice_viewer.TileFormat getTileFormat() {
		return tileFormat;
	}

	public void setSliceCount(int sliceCount) {
		this.sliceCount = sliceCount;
	}

	public void setTileFormat(org.janelia.it.workstation.gui.slice_viewer.TileFormat tileFormat) {
		this.tileFormat = tileFormat;
	}

	public org.janelia.it.workstation.gui.camera.Camera3d getCamera() {
		return camera;
	}

	public void setCamera(org.janelia.it.workstation.gui.camera.Camera3d camera) {
		this.camera = camera;
	}

}
