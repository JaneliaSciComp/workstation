package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.MouseEvent;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Anchor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Skeleton;

public class TraceMode extends BasicMouseMode implements MouseMode 
{
	private Skeleton skeleton;
	
	public TraceMode(Skeleton skeleton) {
		this.skeleton = skeleton;
		setHoverCursor(BasicMouseMode.createCursor("nib.png", 7, 0));
		setDragCursor(BasicMouseMode.createCursor("crosshair.png", 7, 7));
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		super.mouseClicked(event);
		// Only want left/near clicks
		if (event.getButton() == MouseEvent.BUTTON1) {
			Vec3 xyz = worldFromPixel(event.getPoint());
			// System.out.println("Trace click "+xyz);
			skeleton.addAnchorAtXyz(xyz);
		}
	}
	
	// Amplify size of anchor when hovered
	@Override
	public void mouseMoved(MouseEvent event) {
		super.mouseMoved(event);
		Vec3 xyz = worldFromPixel(event.getPoint());
		int pixelRadius = 3;
		double worldRadius = pixelRadius / camera.getPixelsPerSceneUnit();
		// TODO - if this gets slow, use a more efficient search structure, like an octree
		// Find smallest squared distance
		double cutoff = worldRadius * worldRadius;
		double minDist2 = 10 * cutoff; // start too big
		Anchor closest = null;
		for (Anchor a : skeleton.getAnchors()) {
			double d2 = (a.getLocation().minus(xyz)).normSqr();
			if (d2 > cutoff)
				continue;
			if (d2 >= minDist2)
				continue;
			minDist2 = d2;
			closest = a;
		}
		if (closest == null)
			return;
		System.out.println("Found close anchor");
		// TODO
	}

}
