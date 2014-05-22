package org.janelia.it.workstation.gui.slice_viewer.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.janelia.it.workstation.signal.Signal1;

// PanModeAction puts the slice viewer into Pan mode.
public class ZScanScrollModeAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	WheelMode zScanMode;

    public Signal1<WheelMode.Mode> setWheelModeSignal = new Signal1<WheelMode.Mode>();

	public ZScanScrollModeAction() {
		putValue(NAME, "Z-Scan");
		putValue(SMALL_ICON, org.janelia.it.workstation.gui.util.Icons.getIcon("z_stack.png"));
		putValue(SHORT_DESCRIPTION, 
				"Set scroll wheel mode to reveal different image slices.");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		setWheelModeSignal.emit(WheelMode.Mode.SCAN);
		putValue(SELECTED_KEY, true);
	}
}
