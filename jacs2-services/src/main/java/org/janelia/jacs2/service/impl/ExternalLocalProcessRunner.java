package org.janelia.jacs2.service.impl;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.service.qualifier.LocalJob;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@LocalJob
public class ExternalLocalProcessRunner implements ExternalProcessRunner {
    @Named("SLF4J")
    @Inject
    private Logger logger;
    @PropertyValue(name = "service.DefaultWorkingDir")
    @Inject
    private String defaultWorkingDir;

    @Override
    public <R> CompletionStage<JacsService<R>> runCmd(String cmd, List<String> cmdArgs, Map<String, String> env,
                                                      ExternalProcessOutputHandler outStreamHandler,
                                                      ExternalProcessOutputHandler errStreamHandler,
                                                      JacsService<R> serviceContext) {
        logger.debug("Begin local process invocation for {}", serviceContext);
        ProcessBuilder processBuilder = new ProcessBuilder(ImmutableList.<String>builder().add(cmd).addAll(cmdArgs).build());
        if (StringUtils.isNotBlank(serviceContext.getJacsServiceData().getWorkspace())) {
            File workingDirectory = new File(serviceContext.getJacsServiceData().getWorkspace());
            processBuilder.directory(workingDirectory);
        } else if (StringUtils.isNotBlank(defaultWorkingDir)) {
            File workingDirectory = new File(defaultWorkingDir, serviceContext.getJacsServiceData().getName());
            processBuilder.directory(workingDirectory);
        }
        processBuilder.environment().putAll(env);
        logger.info("Start {} with {}; env={}", serviceContext, cmdArgs, processBuilder.environment());
        CompletableFuture<JacsService<R>> completableFuture = new CompletableFuture<>();
        Process localProcess;
        try {
            localProcess = processBuilder.start();
            ExternalProcessIOHandler processStdoutHandler = new ExternalProcessIOHandler(outStreamHandler, localProcess.getInputStream());
            ExternalProcessIOHandler processStderrHandler = new ExternalProcessIOHandler(outStreamHandler, localProcess.getInputStream());
            processStdoutHandler.start();
            processStderrHandler.start();
            try {
                int returnCode = localProcess.waitFor();
                processStdoutHandler.join();
                processStderrHandler.join();
                logger.info("Process {} for {} terminated with code {}", localProcess, serviceContext, returnCode);
                if (returnCode != 0) {
                    completableFuture.completeExceptionally(new ComputationException(serviceContext, "Process terminated with code " + returnCode));
                } else if (processStdoutHandler.getResult() != null) {
                    completableFuture.completeExceptionally(new ComputationException(serviceContext, "Process error: " + processStdoutHandler.getResult()));
                } else if (processStderrHandler.getResult() != null) {
                    completableFuture.completeExceptionally(new ComputationException(serviceContext, "Process error: " + processStderrHandler.getResult()));
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
