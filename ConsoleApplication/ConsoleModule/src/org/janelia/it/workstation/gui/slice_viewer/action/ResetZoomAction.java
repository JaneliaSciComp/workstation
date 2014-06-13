package org.janelia.it.workstation.gui.slice_viewer.action;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.slice_viewer.TileConsumer;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d;

public class ResetZoomAction extends AbstractAction {
	private List<TileConsumer> widgets;
	private VolumeImage3d volumeImage;

	public ResetZoomAction(
			List<TileConsumer> widgets,
			VolumeImage3d volumeImage)
	{
		this.widgets = widgets;
		this.volumeImage = volumeImage;
		putValue(NAME, "Zoom Min");
		putValue(SHORT_DESCRIPTION,
				"Zoom out to show entire volume.");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (volumeImage == null)
			return;
		if (volumeImage.getBoundingBox3d().isEmpty())
			return;
		if (widgets == null)
			return;
		double newZoom = Double.POSITIVE_INFINITY;
		// TODO - assumes all viewers share one camera
		Camera3d camera = null;
		for (TileConsumer viewer : widgets) {
			if (camera == null)
				camera = viewer.getCamera();
			if (! viewer.isShowing())
				continue;
			double z = getZoom(viewer);
			if (Double.isNaN(z))
				continue;
			if (z <= 0)
				continue;
			newZoom = Math.min(z, newZoom); // TODO min? right?
		}
		if (newZoom <= 0)
			return;
		if (Double.isNaN(newZoom))
			return;
		if (newZoom == Double.POSITIVE_INFINITY)
			return;
		camera.setPixelsPerSceneUnit(newZoom);
	}

	private double getZoom(TileConsumer viewer) {
		// Need to fit entire bounding box...
		BoundingBox3d box = volumeImage.getBoundingBox3d();
		Vec3 boxSize = new Vec3(box.getWidth(), box.getHeight(), box.getDepth());
		// ...plus offset from center
		Vec3 bc = box.getCenter();
		Vec3 focus = viewer.getCamera().getFocus();
		Vec3 offset = bc.minus(focus).times(2.0);
		for (int i : new int[] {0,1,2}) {
			offset.set(i, Math.abs(offset.get(i)));
		}
		boxSize = boxSize.plus(offset);
		// Rotate from world to screen coordinates
		boxSize = viewer.getViewerInGround().inverse().times(boxSize);
		// Use minimum of X and Y zoom
		int w = viewer.getViewport().getWidth();
		int h = viewer.getViewport().getHeight();
		if (w <= 0)
			return Double.NaN;
		if (h <= 0)
			return Double.NaN;
		double zx = w / boxSize.get(0);
		double zy = h / boxSize.get(1);
		double zoom = Math.min(zx, zy);
		return zoom;
	}

}
