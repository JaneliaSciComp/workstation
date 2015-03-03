package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.MouseWheelModeListener;

import org.janelia.it.workstation.gui.util.Icons;

public class ZScanScrollModeAction extends AbstractAction
{
	private static final long serialVersionUID = 1L;
	WheelMode zScanMode;
    private MouseWheelModeListener mwmListener;

	public ZScanScrollModeAction() {
		putValue(NAME, "Z-Scan");
		putValue(SMALL_ICON, Icons.getIcon("z_stack.png"));
		putValue(SHORT_DESCRIPTION, 
				"Set scroll wheel mode to reveal different image slices.");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
        mwmListener.setMode(WheelMode.Mode.SCAN);
		putValue(SELECTED_KEY, true);
	}

    /**
     * @param mwmListener the mwmListener to set
     */
    public void setMwmListener(MouseWheelModeListener mwmListener) {
        this.mwmListener = mwmListener;
    }
}
