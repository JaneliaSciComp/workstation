package org.janelia.it.workstation.gui.large_volume_viewer.action;

import org.janelia.it.workstation.gui.util.Icons;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;

/**
 * Change between separate X/Y/Z views and a single Z view.
 * @author brunsc
 *
 */
public class OrthogonalModeAction extends AbstractAction 
{
    private QuadViewUi ui;
    
	public enum OrthogonalMode {
		ORTHOGONAL, Z_VIEW
	}
    
	private OrthogonalMode mode;
	
	public OrthogonalModeAction(QuadViewUi ui) {
        this.ui = ui;
		setZViewMode();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// System.out.println("Orthogonal Mode action performed");
		if (mode == OrthogonalMode.Z_VIEW) {
			setOrthogonalMode();
		}
		else if (mode == OrthogonalMode.ORTHOGONAL) {
			setZViewMode();
		}
        ui.setOrthogonalMode(mode);
	}
	
	private void setZViewMode() {
		mode = OrthogonalMode.Z_VIEW;
		// Modify values so next trigger activates OPPOSITE mode.
		// System.out.println("Set Z View Mode");
		putValue(NAME, "Show Orthogonal Views");
		putValue(SMALL_ICON, Icons.getIcon("OrthogonalMode.png"));
		putValue(SHORT_DESCRIPTION, 
				"Show orthogonal X/Y/Z slice views.");
	}

	private void setOrthogonalMode() {
		mode = OrthogonalMode.ORTHOGONAL;
		// Modify values so next trigger activates OPPOSITE mode.
		// System.out.println("Set Orthogonal Mode");
		putValue(NAME, "Show Z Slice View");
		putValue(SMALL_ICON, Icons.getIcon("ZViewMode.png"));
		putValue(SHORT_DESCRIPTION, 
				"View slices down Z axis.");
	}

}
