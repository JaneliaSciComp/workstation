package org.janelia.jacs2.service.impl;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Named("localProcessRunner")
public class ExternalLocalProcessRunner implements ExternalProcessRunner {
    @Named("SLF4J")
    @Inject
    private Logger logger;

    @Override
    public CompletionStage<JacsServiceData> runCmd(String cmd, List<String> cmdArgs, Map<String, String> env, JacsServiceData serviceContext) {
        logger.debug("Begin local process invocation for {}", serviceContext);
        ProcessBuilder processBuilder = new ProcessBuilder(ImmutableList.<String>builder().add(cmd).addAll(cmdArgs).build());
        processBuilder.inheritIO();
        processBuilder.environment().putAll(env);
        logger.info("Start {} with {}; env={}", serviceContext, cmdArgs, processBuilder.environment());
        CompletableFuture<JacsServiceData> completableFuture = new CompletableFuture<>();
        Process localProcess;
        try {
            localProcess = processBuilder.start();
            try {
                int returnCode = localProcess.waitFor();
                logger.info("Process {} for {} terminated with code {}", localProcess, serviceContext, returnCode);
                if (returnCode != 0) {
                    completableFuture.completeExceptionally(new ComputationException("Process terminated with code " + returnCode));
                } else {
                    completableFuture.complete(serviceContext);
                }
            } catch (InterruptedException e) {
                logger.error("Error waiting for the process {} with {}", serviceContext, cmdArgs);
                completableFuture.completeExceptionally(e);
            }
        } catch (IOException e) {
            logger.error("Error starting the computation process for {} with {}", serviceContext, cmdArgs);
            completableFuture.completeExceptionally(e);
        }
        return completableFuture;
    }
}
