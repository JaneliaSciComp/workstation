package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.MouseEvent;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
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

}
