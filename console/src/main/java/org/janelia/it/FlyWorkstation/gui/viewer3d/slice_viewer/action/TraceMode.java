package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.MouseEvent;

public class TraceMode extends BasicMouseMode implements MouseMode 
{
	public TraceMode() {
		setHoverCursor(BasicMouseMode.createCursor("nib.png", 7, 0));
		setDragCursor(BasicMouseMode.createCursor("crosshair.png", 7, 7));
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		super.mouseClicked(event);
		System.out.println("Trace click");
	}

}
