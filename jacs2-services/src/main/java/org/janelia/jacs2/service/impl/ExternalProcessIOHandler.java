package org.janelia.jacs2.service.impl;

import java.io.InputStream;

class ExternalProcessIOHandler extends Thread {
    private ExternalProcessOutputHandler processOutputHandler;
    private InputStream processOutput;
    private String result;

    ExternalProcessIOHandler(ExternalProcessOutputHandler processOutputHandler, InputStream processOutput) {
        this.processOutputHandler = processOutputHandler;
        this.processOutput = processOutput;
    }

    public void run() {
        result = processOutputHandler.handle(processOutput);
    }

    String getResult() {
        return result;
    }
}
