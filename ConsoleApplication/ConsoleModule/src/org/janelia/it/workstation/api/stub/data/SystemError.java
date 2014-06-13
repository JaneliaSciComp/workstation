package org.janelia.it.workstation.api.stub.data;

public class SystemError extends RuntimeException {

    public SystemError() {
    }

    public SystemError(String msg) {
        super(msg);
    }

}
