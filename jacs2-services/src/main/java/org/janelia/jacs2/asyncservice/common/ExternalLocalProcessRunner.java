package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.MapUtils;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.asyncservice.qualifier.LocalJob;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@LocalJob
public class ExternalLocalProcessRunner extends AbstractExternalProcessRunner {

    @Inject
    public ExternalLocalProcessRunner(Logger logger) {
        super(logger);
    }

    @Override
    public void runCmds(ExternalCodeBlock externalCode,
                        Map<String, String> env,
                        String workingDirName,
                        ExternalProcessOutputHandler outStreamHandler,
                        ExternalProcessOutputHandler errStreamHandler,
                        JacsServiceData serviceContext) {
        logger.debug("Begin local process invocation for {}", serviceContext);
        String processingScript;
        try {
            processingScript = createProcessingScript(externalCode, workingDirName, serviceContext);
        } catch (Exception e) {
            logger.error("Error creating the processing script with {} for {}", externalCode, serviceContext, e);
            throw new ComputationException(serviceContext, e);
        }
        ProcessBuilder processBuilder = new ProcessBuilder(ImmutableList.<String>builder().add(processingScript).build());
        if (MapUtils.isNotEmpty(env)) {
            processBuilder.environment().putAll(env);
        }
        logger.info("Start {} using {} with content={}; env={}", serviceContext, processingScript, externalCode, processBuilder.environment());
        Process localProcess;
        try {
            localProcess = processBuilder.start();
        } catch (Exception e) {
            logger.error("Error starting the computation process {} for {}", processingScript, serviceContext, e);
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
            logger.info("Process {} for {} terminated with code {}", processingScript, serviceContext, returnCode);
            if (returnCode != 0) {
                throw new ComputationException(serviceContext, "Process terminated with code " + returnCode);
            } else if (processStdoutHandler.getResult() != null) {
                throw new ComputationException(serviceContext, "Process error: " + processStdoutHandler.getResult());
            } else if (processStderrHandler.getResult() != null) {
                throw new ComputationException(serviceContext, "Process error: " + processStderrHandler.getResult());
            }
            deleteProcessingScript(processingScript);
        } catch (InterruptedException e) {
            logger.error("Process {} for {} was interrupted", processingScript, serviceContext, e);
            throw new ComputationException(serviceContext, e);
        }
    }

}
