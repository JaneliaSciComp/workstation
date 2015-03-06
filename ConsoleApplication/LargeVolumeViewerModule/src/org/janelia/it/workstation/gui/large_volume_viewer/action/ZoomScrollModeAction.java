package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.MouseWheelModeListener;

import org.janelia.it.workstation.gui.util.Icons;

public class ZoomScrollModeAction extends AbstractAction
{
	protected ZoomMode zoomMode = new ZoomMode();
    private MouseWheelModeListener mwmListener;

	public ZoomScrollModeAction() {
		putValue(NAME, "Zoom");
		putValue(SMALL_ICON, Icons.getIcon("magnify_glass.png"));
		putValue(SHORT_DESCRIPTION, 
				"Set scroll wheel mode to Zoom in and out."
				+ "\n (hold SHIFT key to activate)");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
        mwmListener.setMode(WheelMode.Mode.ZOOM);
		putValue(SELECTED_KEY, true); // this mode is now selected
	}

    /**
     * @param mwmListener the mwmListener to set
     */
    public void setMwmListener(MouseWheelModeListener mwmListener) {
        this.mwmListener = mwmListener;
    }
}
