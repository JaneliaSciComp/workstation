package org.janelia.it.FlyWorkstation.signal;

import java.util.Observable;

import org.janelia.it.FlyWorkstation.gui.slice_viewer.BasicSignalSlot;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.BasicSignalSlot1;


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

	public void disconnect(BasicSignalSlot1<T> dest) {
		deleteObserver(dest);
	}

	public void disconnect(BasicSignalSlot noArgDest) {
		deleteObserver(noArgDest);
	}
	
	// Argument can be ignored by the listener with this version of connect()
	public void connect(BasicSignalSlot noArgListener) {
		addObserver(noArgListener);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void update(Observable o, Object arg) {
		emit((T) arg);	
	}

}
