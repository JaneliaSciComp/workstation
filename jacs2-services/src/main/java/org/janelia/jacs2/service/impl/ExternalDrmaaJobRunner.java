package org.janelia.jacs2.service.impl;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Named("gridRunner")
public class ExternalDrmaaJobRunner implements ExternalProcessRunner {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private SessionFactory drmaaSessionFactory;

    @Override
    public CompletionStage<JacsServiceData> runCmd(String cmd, List<String> cmdArgs, Map<String, String> env, JacsServiceData serviceContext) {
        logger.debug("Begin DRMAA job invocation for {}", serviceContext);
        Session drmaaSession = drmaaSessionFactory.getSession();
        JobTemplate jt = null;
        CompletableFuture<JacsServiceData> completableFuture = new CompletableFuture<>();
        try {
            jt = drmaaSession.createJobTemplate();
            jt.setJobName(serviceContext.getName());
            jt.setRemoteCommand(cmd);
            jt.setArgs(serviceContext.getArgs());
            jt.setWorkingDirectory(serviceContext.getWorkspace());
            jt.setJobEnvironment(env);
            jt.setInputPath(":" + serviceContext.getInputPath());
            jt.setOutputPath(":" + serviceContext.getOutputPath());
            jt.setErrorPath(":" + serviceContext.getErrorPath());
            String jobId = drmaaSession.runJob(jt);
            logger.info("Submitted job {} for {}", jobId, serviceContext);
            drmaaSession.deleteJobTemplate(jt);
            jt = null;
            JobInfo jobInfo = drmaaSession.wait(jobId, Session.TIMEOUT_WAIT_FOREVER);

            if (jobInfo.wasAborted()) {
                logger.error("Job {} for {} never ran", jobId, serviceContext);
                completableFuture.completeExceptionally(new IllegalStateException(String.format("Job %s never ran", jobId)));
            } else if (jobInfo.hasExited()) {
                logger.info("Job {} for {} completed with exist status {}", jobId, serviceContext, jobInfo.getExitStatus());
                if (jobInfo.getExitStatus() != 0) {
                    completableFuture.completeExceptionally(new IllegalStateException(String.format("Job %s completed with status %d", jobId, jobInfo.getExitStatus())));
                } else {
                    completableFuture.complete(serviceContext);
                }
            } else if (jobInfo.hasSignaled()) {
                logger.warn("Job {} for {} terminated due to signal {}", jobId, serviceContext, jobInfo.getTerminatingSignal());
                completableFuture.completeExceptionally(new IllegalStateException(String.format("Job %s completed with status %s", jobId, jobInfo.getTerminatingSignal())));
            } else {
                logger.warn("Job {} for {} finished with unclear conditions", jobId, serviceContext);
                completableFuture.completeExceptionally(new IllegalStateException(String.format("Job %s completed with unclear conditions", jobId)));
            }
        } catch (DrmaaException e) {
            logger.error("Error running a DRMAA job for {} with {}", serviceContext, cmdArgs, e);
            completableFuture.completeExceptionally(e);
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
}
