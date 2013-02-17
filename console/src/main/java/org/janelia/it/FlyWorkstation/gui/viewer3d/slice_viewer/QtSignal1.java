package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Observable;


// Java Observable that acts a bit like a Qt Signal
public class QtSignal1<T extends Object> 
extends Observable
implements QtBasicSignalSlot1<T>
{
	public void emit(T arg) {
		setChanged();
		notifyObservers(arg);
	}
	
	public void connect(QtBasicSignalSlot1<T> dest) {
		addObserver(dest);
	}

	@Override
	public void update(Observable o, Object arg) {
		emit((T) arg);	
	}
}
