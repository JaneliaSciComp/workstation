package org.janelia.jacs2.service.impl;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.janelia.jacs2.model.service.TaskInfo;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractDrmaaJobComputation extends AbstractExternalProcessComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private SessionFactory drmaaSessionFactory;
    private org.ggf.drmaa.JobInfo jobInfo;

    @Override
    public CompletionStage<TaskInfo> processData(TaskInfo taskInfo) {
        logger.debug("Begin DRMAA job invocation for {}", taskInfo);
        List<String> cmdLine = prepareCommandLine(taskInfo);
        Map<String, String> env = prepareEnvironment(taskInfo);
        Session drmaaSession = drmaaSessionFactory.getSession();
        JobTemplate jt = null;
        CompletableFuture<TaskInfo> completableFuture = new CompletableFuture<>();
        try {
            jt = drmaaSession.createJobTemplate();
            jt.setJobName(taskInfo.getName());
            jt.setRemoteCommand(taskInfo.getServiceCmd());
            jt.setWorkingDirectory(taskInfo.getWorkspace());
            jt.setJobEnvironment(env);
            jt.setInputPath(":" + taskInfo.getInputPath());
            jt.setOutputPath(":" + taskInfo.getOutputPath());
            jt.setErrorPath(":" + taskInfo.getErrorPath());
            String jobId = drmaaSession.runJob(jt);
            logger.info("Submitted job {} for {}", jobId, taskInfo);
            drmaaSession.deleteJobTemplate(jt);
            jt = null;
            jobInfo = drmaaSession.wait(jobId, Session.TIMEOUT_WAIT_FOREVER);

            if (jobInfo.wasAborted()) {
                logger.error("Job {} for {} never ran", jobId, taskInfo);
                completableFuture.completeExceptionally(new IllegalStateException(String.format("Job %s never ran", jobId)));
            } else if (jobInfo.hasExited()) {
                logger.info("Job {} for {} completed with exist status {}", jobId, taskInfo, jobInfo.getExitStatus());
                if (jobInfo.getExitStatus() != 0) {
                    completableFuture.completeExceptionally(new IllegalStateException(String.format("Job %s completed with status %d", jobId, jobInfo.getExitStatus())));
                } else {
                    completableFuture.complete(taskInfo);
                }
            } else if (jobInfo.hasSignaled()) {
                logger.warn("Job {} for {} terminated due to signal {}", jobId, taskInfo, jobInfo.getTerminatingSignal());
                completableFuture.completeExceptionally(new IllegalStateException(String.format("Job %s completed with status %s", jobId, jobInfo.getTerminatingSignal())));
            } else {
                logger.warn("Job {} for {} finished with unclear conditions", jobId, taskInfo);
                completableFuture.completeExceptionally(new IllegalStateException(String.format("Job %s completed with unclear conditions", jobId)));
            }
        } catch (DrmaaException e) {
            logger.error("Error running a DRMAA job for {} with {}", taskInfo, cmdLine, e);
            completableFuture.completeExceptionally(e);
        } finally {
            if (jt != null) {
                try {
                    drmaaSession.deleteJobTemplate(jt);
                } catch (DrmaaException e) {
                    logger.warn("Error deleting the DRMAA job template for {} with {}", taskInfo, cmdLine, e);
                }
            }
        }
        return completableFuture;
    }

}
