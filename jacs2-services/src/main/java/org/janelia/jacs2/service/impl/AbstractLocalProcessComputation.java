package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.TaskInfo;
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
    protected TaskInfo doWork(TaskInfo si) throws ComputationException {
        List<String> cmdLine = prepareCommandLine(si);
        Map<String, String> env = prepareEnvironment(si);
        ProcessBuilder processBuilder = new ProcessBuilder(cmdLine);
        processBuilder.inheritIO();
        processBuilder.environment().putAll(env);
        logger.info("Start {}; env={}", cmdLine, processBuilder.environment());
        try {
            localProcess = processBuilder.start();
        } catch (IOException e) {
            logger.error("Error starting the computation process for {}", cmdLine);
            throw new ComputationException(e);
        }
        int returnCode = 1;
        try {
            returnCode = localProcess.waitFor();
        } catch (InterruptedException e) {
            logger.error("Error waiting for the computation process for {}", cmdLine);
            throw new ComputationException(e);
        }
        logger.info("Process {} terminated with code {}", localProcess, returnCode);
        if (returnCode != 0) {
            throw new ComputationException("Process terminated with code " + returnCode);
        }
        return si;
    }

}
