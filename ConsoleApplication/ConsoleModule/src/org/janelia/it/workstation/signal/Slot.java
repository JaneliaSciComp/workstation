package org.janelia.it.workstation.signal;

import java.util.Observable;

import org.janelia.it.workstation.gui.large_volume_viewer.BasicSignalSlot;

public abstract class Slot
implements BasicSignalSlot
{
	// Override this execute() method for your particular slot
	public abstract void execute();

	@Override
	public void update(Observable o, Object arg) {
		execute();	
	}
}
