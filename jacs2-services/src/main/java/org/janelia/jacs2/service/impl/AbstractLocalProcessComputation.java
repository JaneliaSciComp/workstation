package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractLocalProcessComputation extends AbstractExternalProcessComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    private Process localProcess;

    @Override
    public CompletionStage<JacsServiceData> processData(JacsServiceData jacsServiceData) {
        logger.debug("Begin local process invocation for {}", jacsServiceData);
        List<String> cmdLine = prepareCommandLine(jacsServiceData);
        Map<String, String> env = prepareEnvironment(jacsServiceData);
        ProcessBuilder processBuilder = new ProcessBuilder(cmdLine);
        processBuilder.inheritIO();
        processBuilder.environment().putAll(env);
        logger.info("Start {} with {}; env={}", jacsServiceData, cmdLine, processBuilder.environment());
        CompletableFuture<JacsServiceData> completableFuture = new CompletableFuture<>();
        try {
            localProcess = processBuilder.start();
        } catch (IOException e) {
            logger.error("Error starting the computation process for {} with {}", jacsServiceData, cmdLine);
            completableFuture.completeExceptionally(e);
        }
        int returnCode = 1;
        try {
            returnCode = localProcess.waitFor();
        } catch (InterruptedException e) {
            logger.error("Error waiting for the process {} with {}", jacsServiceData, cmdLine);
            completableFuture.completeExceptionally(e);
        }
        logger.info("Process {} for {} terminated with code {}", localProcess, jacsServiceData, returnCode);
        if (returnCode != 0) {
            completableFuture.completeExceptionally(new ComputationException("Process terminated with code " + returnCode));
        } else {
            completableFuture.complete(jacsServiceData);
        }
        return completableFuture;
    }

}
