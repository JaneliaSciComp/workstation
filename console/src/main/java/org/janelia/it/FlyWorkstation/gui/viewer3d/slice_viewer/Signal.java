package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Observable;


// Java Observable that acts a bit like a Qt Signal
public class Signal
extends Observable
implements BasicSignalSlot
{
	public void emit() {
		setChanged();
		notifyObservers();
	}
	
	public void connect(BasicSignalSlot dest) {
		addObserver(dest);
	}

	@Override
	public void update(Observable o, Object arg) {
		emit();	
	}
}
