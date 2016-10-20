package org.janelia.it.workstation.gui.framework.outline;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EventObject;

public class DataReadyEvent extends EventObject {

    public DataReadyEvent(Object source) {
        super(source);
    }

    /**
     * Throws NotSerializableException, since DataReadyEvent objects are not
     * intended to be serializable.
     */
    private void writeObject(ObjectOutputStream out) throws NotSerializableException {
        throw new NotSerializableException("Not serializable.");
    }

    /**
     * Throws NotSerializableException, since DataReadyEvent objects are not
     * intended to be serializable.
     */
    private void readObject(ObjectInputStream in) throws NotSerializableException {
        throw new NotSerializableException("Not serializable.");
    }

    // Defined so that this class isn't flagged as a potential problem when
    // searches for missing serialVersionUID fields are done.
    private static final long serialVersionUID = 793724513368024975L;
}