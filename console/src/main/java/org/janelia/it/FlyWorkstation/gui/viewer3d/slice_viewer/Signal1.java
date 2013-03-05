package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Observable;


// Java Observable that acts a bit like a Qt Signal
public class Signal1<T extends Object> 
extends Observable
implements BasicSignalSlot1<T>
{
	public void emit(T arg) {
		setChanged();
		notifyObservers(arg);
	}
	
	public void connect(BasicSignalSlot1<T> dest) {
		addObserver(dest);
	}

	// Argument can be ignored by the listener with this version of connect()
	public void connect(BasicSignalSlot noArgListener) {
		addObserver(noArgListener);
	}

	@Override
	public void update(Observable o, Object arg) {
		emit((T) arg);	
	}

}
