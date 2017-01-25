package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.asyncservice.qualifier.LocalJob;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@LocalJob
public class ExternalLocalProcessRunner implements ExternalProcessRunner {
    private final Logger logger;

    @Inject
    public ExternalLocalProcessRunner(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void runCmd(String cmd, List<String> cmdArgs, Map<String, String> env, String workingDirName,
                       ExternalProcessOutputHandler outStreamHandler, ExternalProcessOutputHandler errStreamHandler, JacsServiceData serviceContext) {
        logger.debug("Begin local process invocation for {}", serviceContext);
        ProcessBuilder processBuilder = new ProcessBuilder(ImmutableList.<String>builder().add(cmd).addAll(cmdArgs).build());
        processBuilder.environment().putAll(env);
        logger.info("Start {} with {} {}; env={}", serviceContext, cmd, cmdArgs, processBuilder.environment());
        Process localProcess;
        try {
            if (StringUtils.isNotBlank(workingDirName)) {
                File workingDirectory = new File(workingDirName);
                Files.createDirectories(workingDirectory.toPath());
                processBuilder.directory(workingDirectory);
            }
            localProcess = processBuilder.start();
        } catch (Exception e) {
            logger.error("Error starting the computation process for {} with {}", serviceContext, cmdArgs, e);
            throw new ComputationException(serviceContext, e);
        }
        ExternalProcessIOHandler processStdoutHandler = new ExternalProcessIOHandler(outStreamHandler, localProcess.getInputStream());
        processStdoutHandler.start();
        ExternalProcessIOHandler processStderrHandler = new ExternalProcessIOHandler(errStreamHandler, localProcess.getErrorStream());
        processStderrHandler.start();
        try {
            int returnCode = localProcess.waitFor();
            processStdoutHandler.join();
            processStderrHandler.join();
            logger.info("Process {} {}; env={} for {} terminated with code {}", cmd, cmdArgs, processBuilder.environment(), serviceContext, returnCode);
            if (returnCode != 0) {
                throw new ComputationException(serviceContext, "Process terminated with code " + returnCode);
            } else if (processStdoutHandler.getResult() != null) {
                throw new ComputationException(serviceContext, "Process error: " + processStdoutHandler.getResult());
            } else if (processStderrHandler.getResult() != null) {
                throw new ComputationException(serviceContext, "Process error: " + processStderrHandler.getResult());
            }
        } catch (InterruptedException e) {
            logger.error("Error waiting for the process {} with {}", serviceContext, cmdArgs, e);
            throw new ComputationException(serviceContext, e);
        }
    }
}
