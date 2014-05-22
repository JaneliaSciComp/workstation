package org.janelia.it.workstation.gui.framework.outline;

import java.io.NotSerializableException;

public class DataReadyEvent extends java.util.EventObject {

    public DataReadyEvent(Object source) {
        super(source);
    }

    /**
     * Throws NotSerializableException, since DataReadyEvent objects are not
     * intended to be serializable.
     */
    private void writeObject(java.io.ObjectOutputStream out) throws NotSerializableException {
        throw new NotSerializableException("Not serializable.");
    }

    /**
     * Throws NotSerializableException, since DataReadyEvent objects are not
     * intended to be serializable.
     */
    private void readObject(java.io.ObjectInputStream in) throws NotSerializableException {
        throw new NotSerializableException("Not serializable.");
    }

    // Defined so that this class isn't flagged as a potential problem when
    // searches for missing serialVersionUID fields are done.
    private static final long serialVersionUID = 793724513368024975L;
}