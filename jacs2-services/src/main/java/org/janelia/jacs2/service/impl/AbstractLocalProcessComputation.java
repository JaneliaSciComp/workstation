package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.ServiceInfo;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class AbstractLocalProcessComputation extends AbstractExternalProcessComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    private Process localProcess;

    @Override
    protected void performComputation() {
        List<String> cmdLine = prepareCommandLine();
        Map<String, String> env = prepareEnvironment();
        ProcessBuilder processBuilder = new ProcessBuilder(cmdLine);
        processBuilder.inheritIO();
        processBuilder.environment().putAll(env);
        logger.info("Start {}; env={}", cmdLine, processBuilder.environment());
        try {
            localProcess = processBuilder.start();
        } catch (IOException e) {
            logger.error("Error starting the computation process for {}", cmdLine);
            throw new IllegalStateException(e);
        }
        int returnCode = 1;
        try {
            returnCode = localProcess.waitFor();
        } catch (InterruptedException e) {
            logger.error("Error waiting for the computation process for {}", cmdLine);
            throw new IllegalStateException(e);
        }
        logger.info("Process {} terminated with code {}", localProcess, returnCode);
        if (returnCode != 0) {
            throw new IllegalStateException("Process terminated with code " + returnCode);
        }
    }

}
