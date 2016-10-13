package org.janelia.it.workstation.gui.browser.api.exceptions;

public class SystemError extends RuntimeException {

    public SystemError() {
    }

    public SystemError(String msg) {
        super(msg);
    }

}
