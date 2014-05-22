package org.janelia.it.workstation.gui.slice_viewer;

import java.util.Observable;

public abstract class Slot
implements org.janelia.it.workstation.gui.slice_viewer.BasicSignalSlot
{
	// Override this execute() method for your particular slot
	public abstract void execute();

	@Override
	public void update(Observable o, Object arg) {
		execute();	
	}
}
