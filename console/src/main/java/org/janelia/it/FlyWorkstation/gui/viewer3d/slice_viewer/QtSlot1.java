package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Observable;

public abstract class QtSlot1<T> 
implements QtBasicSignalSlot1<T>
{
	protected Object receiver;

	public QtSlot1(Object receiver) {
		this.receiver = receiver;
	}

	// Override this execute() method for your particular slot
	public abstract void execute(T arg);

	@Override
	public void update(Observable o, Object arg) {
		execute( (T) arg);	
	}
}
