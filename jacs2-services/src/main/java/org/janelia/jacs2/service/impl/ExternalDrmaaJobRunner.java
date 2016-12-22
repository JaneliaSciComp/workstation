package org.janelia.jacs2.service.impl;

import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.qualifier.ClusterJob;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@ClusterJob
public class ExternalDrmaaJobRunner implements ExternalProcessRunner {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @PropertyValue(name = "service.DefaultWorkingDir")
    @Inject
    private String defaultWorkingDir;
    @Inject
    private Session drmaaSession;

    @Override
    public <R> CompletionStage<JacsService<R>> runCmd(String cmd, List<String> cmdArgs, Map<String, String> env,
                                                      ExternalProcessOutputHandler outStreamHandler,
                                                      ExternalProcessOutputHandler errStreamHandler,
                                                      JacsService<R> serviceContext) {
        logger.debug("Begin DRMAA job invocation for {}", serviceContext);
        JobTemplate jt = null;
        CompletableFuture<JacsService<R>> completableFuture = new CompletableFuture<>();
        File outputFile = null;
        File errorFile = null;
        try {
            JacsServiceData serviceData = serviceContext.getJacsServiceData();
            jt = drmaaSession.createJobTemplate();
            jt.setJobName(serviceData.getName());
            jt.setRemoteCommand(cmd);
            jt.setArgs(cmdArgs);
            File workingDirectory = setJobWorkingDirectory(jt, serviceData);
            jt.setJobEnvironment(env);
            if (StringUtils.isNotBlank(serviceData.getInputPath())) {
                jt.setInputPath(":" + serviceData.getInputPath());
            }
            if (StringUtils.isNotBlank(serviceData.getOutputPath())) {
                outputFile = new File(serviceData.getOutputPath());
                Files.createParentDirs(outputFile);
                jt.setOutputPath(":" + outputFile.getAbsolutePath());
            }
            if (StringUtils.isNotBlank(serviceData.getErrorPath())) {
                errorFile = new File(serviceData.getErrorPath());
                Files.createParentDirs(errorFile);
                jt.setErrorPath(":" + errorFile.getAbsolutePath());
            }
            String jobId = drmaaSession.runJob(jt);
            logger.info("Submitted job {} for {} with {} {}; env={}", jobId, serviceContext, cmd, cmdArgs, env);
            drmaaSession.deleteJobTemplate(jt);
            jt = null;
            if (outputFile == null) {
                outputFile = new File(workingDirectory, serviceData.getName() + ".o" + jobId);
            }
            if (errorFile == null) {
                errorFile = new File(workingDirectory, serviceData.getName() + ".e" + jobId);
            }
            JobInfo jobInfo = drmaaSession.wait(jobId, Session.TIMEOUT_WAIT_FOREVER);

            if (jobInfo.wasAborted()) {
                logger.error("Job {} for {} never ran", jobId, serviceContext);
                completableFuture.completeExceptionally(new ComputationException(serviceContext, String.format("Job %s never ran", jobId)));
            } else if (jobInfo.hasExited()) {
                logger.info("Job {} for {} completed with exist status {}", jobId, serviceContext, jobInfo.getExitStatus());
                ExternalProcessIOHandler processStdoutHandler = new ExternalProcessIOHandler(outStreamHandler, new FileInputStream(outputFile));
                processStdoutHandler.run();
                ExternalProcessIOHandler processStderrHandler = new ExternalProcessIOHandler(errStreamHandler, new FileInputStream(errorFile));
                processStderrHandler.run();
                if (jobInfo.getExitStatus() != 0) {
                    completableFuture.completeExceptionally(new ComputationException(serviceContext, String.format("Job %s completed with status %d", jobId, jobInfo.getExitStatus())));
                } else if (processStdoutHandler.getResult() != null) {
                    completableFuture.completeExceptionally(new ComputationException(serviceContext, "Process error: " + processStdoutHandler.getResult()));
                } else if (processStderrHandler.getResult() != null) {
                    completableFuture.completeExceptionally(new ComputationException(serviceContext, "Process error: " + processStderrHandler.getResult()));
                } else {
                    completableFuture.complete(serviceContext);
                }
            } else if (jobInfo.hasSignaled()) {
                logger.warn("Job {} for {} terminated due to signal {}", jobId, serviceContext, jobInfo.getTerminatingSignal());
                completableFuture.completeExceptionally(new ComputationException(serviceContext, String.format("Job %s completed with status %s", jobId, jobInfo.getTerminatingSignal())));
            } else {
                logger.warn("Job {} for {} finished with unclear conditions", jobId, serviceContext);
                completableFuture.completeExceptionally(new ComputationException(serviceContext, String.format("Job %s completed with unclear conditions", jobId)));
            }
        } catch (Exception e) {
            logger.error("Error running a DRMAA job for {} with {}", serviceContext, cmdArgs, e);
            completableFuture.completeExceptionally(new ComputationException(serviceContext, e));
        } finally {
            if (jt != null) {
                try {
                    drmaaSession.deleteJobTemplate(jt);
                } catch (DrmaaException e) {
                    logger.warn("Error deleting the DRMAA job template for {} with {}", serviceContext, cmdArgs, e);
                }
            }
        }
        return completableFuture;
    }

    private File setJobWorkingDirectory(JobTemplate jt, JacsServiceData serviceData) {
        File workingDirectory;
        if (StringUtils.isNotBlank(serviceData.getWorkspace())) {
            workingDirectory = new File(serviceData.getWorkspace());
        } else if (StringUtils.isNotBlank(defaultWorkingDir)) {
            workingDirectory = new File(defaultWorkingDir, serviceData.getName());
        } else {
            workingDirectory = Files.createTempDir();
        }
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }
        if (!workingDirectory.exists()) {
            throw new IllegalStateException("Cannot create working directory " + workingDirectory.getAbsolutePath());
        } else {
            logger.debug("Use '{}' as working directory for {}", workingDirectory, serviceData);
        }
        try {
            jt.setWorkingDirectory(workingDirectory.getAbsolutePath());
        } catch (DrmaaException e) {
            throw new IllegalStateException(e);
        }
        return workingDirectory;
    }
}
