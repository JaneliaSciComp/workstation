package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileFormat;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileIndex;

/**
 * Base class for z slice scanning actions in slice viewer.
 * 
 * @author Christopher M. Bruns
 *
 */
public class ZScanAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	private VolumeImage3d image;
	private Camera3d camera;
	private int sliceCount;
	private TileFormat tileFormat;
	
	ZScanAction(VolumeImage3d image, Camera3d camera, int sliceCount)
	{
		this.image = image;
		this.camera = camera;
		this.sliceCount = sliceCount;		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) 
	{
		Vec3 oldFocus = camera.getFocus();
		double oldZ = oldFocus.getZ();
		int oldZIndex = (int)(Math.round(oldZ / image.getZResolution()) + 0.1);

		// Take larger steps at lower octree zoom levels
		int zoomedSliceCount = sliceCount;
		if ((tileFormat != null) && (tileFormat.getIndexStyle() == TileIndex.IndexStyle.OCTREE))
		{
			double maxRes = Math.min(image.getXResolution(), image.getYResolution());
			double voxelsPerPixel = 1.0 / (camera.getPixelsPerSceneUnit() * maxRes);
			int zoom = 20; // default to very coarse zoom
			if (voxelsPerPixel > 0.0) {
				double topZoom = Math.log(voxelsPerPixel) / Math.log(2.0);
				zoom = (int)(topZoom);
			}
			int zoomMin = 0;
			int zoomMax = tileFormat.getZoomLevelCount() - 1;
			zoom = Math.max(zoom, zoomMin);
			zoom = Math.min(zoom, zoomMax);
			int deltaZ = (int)Math.pow(2, zoom);
			zoomedSliceCount = sliceCount * deltaZ;
		}
		
		int newZIndex = oldZIndex + zoomedSliceCount;
		
		// Scoot to next multiple of 10, for (apparent) performance
		int sliceIncrement = Math.abs(sliceCount);
		if (sliceIncrement == 10) {
			int dz = newZIndex % sliceIncrement;
			if (dz > sliceIncrement/2.0)
				dz -= sliceIncrement;
			newZIndex -= dz; // newZIndex is now a multiple of sliceCount
			assert newZIndex % sliceIncrement == 0;
		}

		double newZ = newZIndex * image.getZResolution();
		double maxZ = image.getBoundingBox3d().getMax().getZ();
		double minZ = image.getBoundingBox3d().getMin().getZ();
		assert maxZ >= minZ;
		if (newZ > maxZ)
			newZ = maxZ;
		if (newZ < minZ)
			newZ = minZ;
		if (newZ == oldZ)
			return; // no change
		Vec3 newFocus = new Vec3(oldFocus.getX(), oldFocus.getY(), newZ);
		// TODO - disallow camera.getFocus().setZ(), which bypasses camera signaling
		camera.setFocus(newFocus);
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
