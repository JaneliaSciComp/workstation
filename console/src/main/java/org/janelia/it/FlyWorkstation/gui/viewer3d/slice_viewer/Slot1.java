package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Observable;

public abstract class Slot1<T> 
implements BasicSignalSlot1<T>
{
	protected Object receiver;

	public Slot1(Object receiver) {
		this.receiver = receiver;
	}

	// Override this execute() method for your particular slot
	public abstract void execute(T arg);

	@Override
	public void update(Observable o, Object arg) {
		execute( (T) arg);	
	}
}
