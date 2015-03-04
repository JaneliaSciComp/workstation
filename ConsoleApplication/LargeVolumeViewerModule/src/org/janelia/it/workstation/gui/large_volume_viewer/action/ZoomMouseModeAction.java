package org.janelia.it.workstation.gui.large_volume_viewer.action;

import org.janelia.it.workstation.gui.util.Icons;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.MouseWheelModeListener;

public class ZoomMouseModeAction extends AbstractAction
{
	private static final long serialVersionUID = 1L;
    private MouseWheelModeListener mwmListener;

	public ZoomMouseModeAction() {
		putValue(NAME, "Zoom");
		putValue(SMALL_ICON, Icons.getIcon("magnify_glass.png"));
		String acc = "Z";
		KeyStroke accelerator = KeyStroke.getKeyStroke(acc);
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(SHORT_DESCRIPTION, 
				"Set mouse mode to Zoom in and out."
				+ "\n (Shortcut: " + acc + ")");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
        mwmListener.setMode(MouseMode.Mode.ZOOM);
		putValue(SELECTED_KEY, true);
	}

    /**
     * @param mwmListener the mwmListener to set
     */
    public void setMwmListener(MouseWheelModeListener mwmListener) {
        this.mwmListener = mwmListener;
    }
}
