package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal1;

/**
 * Change between separate X/Y/Z views and a single Z view.
 * @author brunsc
 *
 */
public class OrthogonalModeAction extends AbstractAction 
{
	public enum OrthogonalMode {
		ORTHOGONAL, Z_VIEW
	}

	public Signal1<OrthogonalMode> orthogonalModeChanged =
			new Signal1<OrthogonalMode>();
	
	private OrthogonalMode mode;
	
	public OrthogonalModeAction() {
		setZViewMode();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// System.out.println("Orthogonal Mode action performed");
		if (mode == OrthogonalMode.Z_VIEW) {
			setOrthogonalMode();
			orthogonalModeChanged.emit(mode);
		}
		else if (mode == OrthogonalMode.ORTHOGONAL) {
			setZViewMode();
			orthogonalModeChanged.emit(mode);
		}
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
