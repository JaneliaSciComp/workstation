package org.janelia.it.workstation.signal;

import java.util.Observable;


// Java Observable that acts a bit like a Qt Signal
public class Signal
extends Observable
implements org.janelia.it.workstation.gui.slice_viewer.BasicSignalSlot
{
	public void emit() {
		setChanged();
		notifyObservers();
	}
	
	public void connect(org.janelia.it.workstation.gui.slice_viewer.BasicSignalSlot dest) {
		addObserver(dest);
	}

	public void disconnect(org.janelia.it.workstation.gui.slice_viewer.BasicSignalSlot dest) {
		deleteObserver(dest);
	}

	@Override
	public void update(Observable o, Object arg) {
		emit();	
	}
}
