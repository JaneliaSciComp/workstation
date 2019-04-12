package org.janelia.workstation.core.api.exceptions;

public class FatalCommError extends SystemError {

    String machineName;

    public FatalCommError() {
    }

    public FatalCommError(String msg) {
        super(msg);
    }

    public FatalCommError(String machineName, String msg) {
        super(msg);
        this.machineName = machineName;
    }

    public String getMachineName() {
        return machineName;
    }

}
