package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.MapUtils;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.asyncservice.qualifier.LocalJob;
import org.janelia.jacs2.model.jacsservice.JacsServiceEventTypes;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.Map;

@LocalJob
public class ExternalLocalProcessRunner extends AbstractExternalProcessRunner {

    @Inject
    public ExternalLocalProcessRunner(JacsServiceDataPersistence jacsServiceDataPersistence, Logger logger) {
        super(jacsServiceDataPersistence, logger);
    }

    @Override
    public void runCmds(ExternalCodeBlock externalCode,
                        Map<String, String> env,
                        String workingDirName,
                        ExternalProcessOutputHandler outStreamHandler,
                        ExternalProcessOutputHandler errStreamHandler,
                        JacsServiceData serviceContext) {
        logger.debug("Begin local process invocation for {}", serviceContext);
        String processingScript = createProcessingScript(externalCode, workingDirName, serviceContext);
        serviceContext.setState(JacsServiceState.RUNNING);
        this.jacsServiceDataPersistence.update(serviceContext);
        ProcessBuilder processBuilder = new ProcessBuilder(ImmutableList.<String>builder()
                .add(processingScript).build());
        if (MapUtils.isNotEmpty(env)) {
            processBuilder.environment().putAll(env);
        }
        Process localProcess;
        try {
            logger.debug("Start {} using {} with content={}; env={}", serviceContext, processingScript, externalCode, env);
            serviceContext.addEvent(JacsServiceEventTypes.START_PROCESS, String.format("Start %s", processingScript));
            localProcess = processBuilder.start();
            logger.info("Started process {} for {}", localProcess, serviceContext);
        } catch (Exception e) {
            logger.error("Error starting the computation process {} for {}", processingScript, serviceContext, e);
            serviceContext.addEvent(JacsServiceEventTypes.START_PROCESS_ERROR, String.format("Error starting %s - %s", processingScript, e.getMessage()));
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
                serviceContext.addEvent(JacsServiceEventTypes.PROCESSING_ERROR, String.format("Error processing %s - %s", processingScript, processStdoutHandler.getResult()));
                throw new ComputationException(serviceContext, "Process error: " + processStdoutHandler.getResult());
            } else if (processStderrHandler.getResult() != null) {
                serviceContext.addEvent(JacsServiceEventTypes.PROCESSING_ERROR, String.format("Error processing %s - %s", processingScript, processStderrHandler.getResult()));
                throw new ComputationException(serviceContext, "Process error: " + processStderrHandler.getResult());
            }
            serviceContext.addEvent(JacsServiceEventTypes.PROCESSING_COMPLETED, String.format("Completed %s", processingScript));
            deleteProcessingScript(processingScript);
        } catch (InterruptedException e) {
            logger.error("Process {} for {} was interrupted", processingScript, serviceContext, e);
            serviceContext.addEvent(JacsServiceEventTypes.PROCESSING_ERROR, String.format("Interrupted processing %s - %s", processingScript, e.getMessage()));
            throw new ComputationException(serviceContext, e);
        } finally {
            this.jacsServiceDataPersistence.update(serviceContext);
        }
    }

}
