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
    protected TaskInfo doWork(TaskInfo taskInfo) throws ComputationException {
        logger.debug("Begin local process invocation for {}", taskInfo);
        List<String> cmdLine = prepareCommandLine(taskInfo);
        Map<String, String> env = prepareEnvironment(taskInfo);
        ProcessBuilder processBuilder = new ProcessBuilder(cmdLine);
        processBuilder.inheritIO();
        processBuilder.environment().putAll(env);
        logger.info("Start {} with {}; env={}", taskInfo, cmdLine, processBuilder.environment());
        try {
            localProcess = processBuilder.start();
        } catch (IOException e) {
            logger.error("Error starting the computation process for {} with {}", taskInfo, cmdLine);
            throw new ComputationException(e);
        }
        int returnCode = 1;
        try {
            returnCode = localProcess.waitFor();
        } catch (InterruptedException e) {
            logger.error("Error waiting for the process {} with {}", taskInfo, cmdLine);
            throw new ComputationException(e);
        }
        logger.info("Process {} for {} terminated with code {}", localProcess, taskInfo, returnCode);
        if (returnCode != 0) {
            throw new ComputationException("Process terminated with code " + returnCode);
        }
        return taskInfo;
    }

}
