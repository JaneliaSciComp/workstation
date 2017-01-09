package org.janelia.jacs2.service.impl;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.qualifier.LocalJob;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@LocalJob
public class ExternalLocalProcessRunner implements ExternalProcessRunner {
    @Inject
    private Logger logger;

    @Override
    public <R> CompletionStage<JacsService<R>> runCmd(String cmd, List<String> cmdArgs, Map<String, String> env,
                                                      String workingDirName,
                                                      ExternalProcessOutputHandler outStreamHandler,
                                                      ExternalProcessOutputHandler errStreamHandler,
                                                      JacsService<R> serviceContext) {
        logger.debug("Begin local process invocation for {}", serviceContext);
        ProcessBuilder processBuilder = new ProcessBuilder(ImmutableList.<String>builder().add(cmd).addAll(cmdArgs).build());
        processBuilder.environment().putAll(env);
        logger.info("Start {} with {} {}; env={}", serviceContext, cmd, cmdArgs, processBuilder.environment());
        CompletableFuture<JacsService<R>> completableFuture = new CompletableFuture<>();
        Process localProcess;
        try {
            if (StringUtils.isNotBlank(workingDirName)) {
                File workingDirectory = new File(workingDirName);
                Files.createDirectories(workingDirectory.toPath());
                processBuilder.directory(workingDirectory);
            }
            localProcess = processBuilder.start();
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
