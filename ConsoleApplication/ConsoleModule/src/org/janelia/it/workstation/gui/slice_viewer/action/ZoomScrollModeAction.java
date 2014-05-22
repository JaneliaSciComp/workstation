package org.janelia.it.workstation.gui.slice_viewer.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.janelia.it.workstation.signal.Signal1;

// PanModeAction puts the slice viewer into Pan mode.
public class ZoomScrollModeAction extends AbstractAction 
{
	protected ZoomMode zoomMode = new ZoomMode();

    public Signal1<org.janelia.it.workstation.gui.slice_viewer.action.WheelMode.Mode> setWheelModeSignal = new Signal1<org.janelia.it.workstation.gui.slice_viewer.action.WheelMode.Mode>();

	public ZoomScrollModeAction() {
		putValue(NAME, "Zoom");
		putValue(SMALL_ICON, org.janelia.it.workstation.gui.util.Icons.getIcon("magnify_glass.png"));
		putValue(SHORT_DESCRIPTION, 
				"Set scroll wheel mode to Zoom in and out."
				+ "\n (hold SHIFT key to activate)");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		setWheelModeSignal.emit(org.janelia.it.workstation.gui.slice_viewer.action.WheelMode.Mode.ZOOM);
		putValue(SELECTED_KEY, true); // this mode is now selected
	}
}
