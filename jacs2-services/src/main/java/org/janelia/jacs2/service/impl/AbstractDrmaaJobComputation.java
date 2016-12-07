package org.janelia.jacs2.service.impl;

import org.ggf.drmaa.DrmaaException;
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

public abstract class AbstractDrmaaJobComputation extends AbstractExternalProcessComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private SessionFactory drmaaSessionFactory;
    private org.ggf.drmaa.JobInfo jobInfo;

    @Override
    public CompletionStage<JacsServiceData> processData(JacsServiceData jacsServiceData) {
        logger.debug("Begin DRMAA job invocation for {}", jacsServiceData);
        List<String> cmdLine = prepareCommandLine(jacsServiceData);
        Map<String, String> env = prepareEnvironment(jacsServiceData);
        Session drmaaSession = drmaaSessionFactory.getSession();
        JobTemplate jt = null;
        CompletableFuture<JacsServiceData> completableFuture = new CompletableFuture<>();
        try {
            jt = drmaaSession.createJobTemplate();
            jt.setJobName(jacsServiceData.getName());
            jt.setRemoteCommand(jacsServiceData.getServiceCmd());
            jt.setWorkingDirectory(jacsServiceData.getWorkspace());
            jt.setJobEnvironment(env);
            jt.setInputPath(":" + jacsServiceData.getInputPath());
            jt.setOutputPath(":" + jacsServiceData.getOutputPath());
            jt.setErrorPath(":" + jacsServiceData.getErrorPath());
            String jobId = drmaaSession.runJob(jt);
            logger.info("Submitted job {} for {}", jobId, jacsServiceData);
            drmaaSession.deleteJobTemplate(jt);
            jt = null;
            jobInfo = drmaaSession.wait(jobId, Session.TIMEOUT_WAIT_FOREVER);

            if (jobInfo.wasAborted()) {
                logger.error("Job {} for {} never ran", jobId, jacsServiceData);
                completableFuture.completeExceptionally(new IllegalStateException(String.format("Job %s never ran", jobId)));
            } else if (jobInfo.hasExited()) {
                logger.info("Job {} for {} completed with exist status {}", jobId, jacsServiceData, jobInfo.getExitStatus());
                if (jobInfo.getExitStatus() != 0) {
                    completableFuture.completeExceptionally(new IllegalStateException(String.format("Job %s completed with status %d", jobId, jobInfo.getExitStatus())));
                } else {
                    completableFuture.complete(jacsServiceData);
                }
            } else if (jobInfo.hasSignaled()) {
                logger.warn("Job {} for {} terminated due to signal {}", jobId, jacsServiceData, jobInfo.getTerminatingSignal());
                completableFuture.completeExceptionally(new IllegalStateException(String.format("Job %s completed with status %s", jobId, jobInfo.getTerminatingSignal())));
            } else {
                logger.warn("Job {} for {} finished with unclear conditions", jobId, jacsServiceData);
                completableFuture.completeExceptionally(new IllegalStateException(String.format("Job %s completed with unclear conditions", jobId)));
            }
        } catch (DrmaaException e) {
            logger.error("Error running a DRMAA job for {} with {}", jacsServiceData, cmdLine, e);
            completableFuture.completeExceptionally(e);
        } finally {
            if (jt != null) {
                try {
                    drmaaSession.deleteJobTemplate(jt);
                } catch (DrmaaException e) {
                    logger.warn("Error deleting the DRMAA job template for {} with {}", jacsServiceData, cmdLine, e);
                }
            }
        }
        return completableFuture;
    }

}
