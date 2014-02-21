package org.janelia.it.FlyWorkstation.signal;

import java.util.Observable;

import org.janelia.it.FlyWorkstation.gui.slice_viewer.BasicSignalSlot1;

public abstract class Slot1<T> 
implements BasicSignalSlot1<T>
{
	public Slot1() {
	}

	// Override this execute() method for your particular slot
	public abstract void execute(T arg);

	@SuppressWarnings("unchecked")
	@Override
	public void update(Observable o, Object arg) {
		execute( (T) arg);	
	}
}
