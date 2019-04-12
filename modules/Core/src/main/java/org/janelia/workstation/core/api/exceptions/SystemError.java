package org.janelia.workstation.core.api.exceptions;

public class SystemError extends RuntimeException {

    public SystemError() {
    }

    public SystemError(String msg) {
        super(msg);
    }

}
