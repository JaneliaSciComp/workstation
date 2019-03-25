package org.janelia.it.workstation.browser.api.exceptions;

public class SystemError extends RuntimeException {

    public SystemError() {
    }

    public SystemError(String msg) {
        super(msg);
    }

}
