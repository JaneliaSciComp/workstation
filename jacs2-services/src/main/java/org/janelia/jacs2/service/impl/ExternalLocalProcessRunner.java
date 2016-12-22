package org.janelia.jacs2.service.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.qualifier.LocalJob;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@LocalJob
public class ExternalLocalProcessRunner implements ExternalProcessRunner {
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
        JacsServiceData serviceData = serviceContext.getJacsServiceData();
        if (StringUtils.isNotBlank(serviceData.getWorkspace())) {
            File workingDirectory = new File(serviceData.getWorkspace());
            processBuilder.directory(workingDirectory);
        } else if (StringUtils.isNotBlank(defaultWorkingDir)) {
            File workingDirectory = new File(defaultWorkingDir, serviceData.getName());
            processBuilder.directory(workingDirectory);
        }
        File outputFile = null;
        File errorFile = null;
        processBuilder.environment().putAll(env);
        logger.info("Start {} with {}; env={}", serviceContext, cmdArgs, processBuilder.environment());
        CompletableFuture<JacsService<R>> completableFuture = new CompletableFuture<>();
        Process localProcess;
        try {
            if (StringUtils.isNotBlank(serviceData.getOutputPath())) {
                outputFile = new File(serviceData.getOutputPath());
                Files.createParentDirs(outputFile);
                processBuilder.redirectOutput(outputFile);
            }
            if (StringUtils.isNotBlank(serviceData.getErrorPath())) {
                errorFile = new File(serviceData.getErrorPath());
                Files.createParentDirs(errorFile);
                processBuilder.redirectError(errorFile);
            }
            localProcess = processBuilder.start();
            ExternalProcessIOHandler processStdoutHandler = null;
            if (outputFile == null) {
                processStdoutHandler = new ExternalProcessIOHandler(outStreamHandler, localProcess.getInputStream());
                processStdoutHandler.start();
            }
            ExternalProcessIOHandler processStderrHandler = null;
            if (errorFile == null) {
                processStderrHandler = new ExternalProcessIOHandler(outStreamHandler, localProcess.getInputStream());
                processStderrHandler.start();
            }
            try {
                int returnCode = localProcess.waitFor();
                if (processStdoutHandler != null) {
                    processStdoutHandler.join();
                } else {
                    processStdoutHandler = new ExternalProcessIOHandler(outStreamHandler, new FileInputStream(outputFile));
                    processStdoutHandler.run();
                }
                if (processStderrHandler != null) {
                    processStderrHandler.join();
                } else {
                    processStderrHandler = new ExternalProcessIOHandler(outStreamHandler, new FileInputStream(errorFile));
                    processStderrHandler.run();
                }
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
