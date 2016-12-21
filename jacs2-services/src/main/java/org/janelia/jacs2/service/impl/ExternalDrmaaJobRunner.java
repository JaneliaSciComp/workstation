package org.janelia.jacs2.service.impl;

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
    private SessionFactory drmaaSessionFactory;

    @Override
    public <R> CompletionStage<JacsService<R>> runCmd(String cmd, List<String> cmdArgs, Map<String, String> env,
                                                      ExternalProcessOutputHandler outStreamHandler,
                                                      ExternalProcessOutputHandler errStreamHandler,
                                                      JacsService<R> serviceContext) {
        logger.debug("Begin DRMAA job invocation for {}", serviceContext);
        Session drmaaSession = drmaaSessionFactory.getSession();
        JobTemplate jt = null;
        CompletableFuture<JacsService<R>> completableFuture = new CompletableFuture<>();
        try {
            drmaaSession.init(null); // initialize DRMAA session
            JacsServiceData serviceData = serviceContext.getJacsServiceData();
            jt = drmaaSession.createJobTemplate();
            jt.setJobName(serviceData.getName());
            jt.setRemoteCommand(cmd);
            jt.setArgs(cmdArgs);
            if (StringUtils.isNotBlank(serviceData.getWorkspace())) {
                jt.setWorkingDirectory(serviceData.getWorkspace());
            } else if (StringUtils.isNotBlank(defaultWorkingDir)) {
                jt.setWorkingDirectory(new File(defaultWorkingDir, serviceData.getName()).getAbsolutePath());
            }
            jt.setJobEnvironment(env);
            if (StringUtils.isNotBlank(serviceData.getInputPath())) {
                jt.setInputPath(":" + serviceData.getInputPath());
            }
            if (StringUtils.isNotBlank(serviceData.getOutputPath())) {
                jt.setOutputPath(":" + serviceData.getOutputPath());
            }
            if (StringUtils.isNotBlank(serviceData.getErrorPath())) {
                jt.setErrorPath(":" + serviceData.getErrorPath());
            }
            String jobId = drmaaSession.runJob(jt);
            logger.info("Submitted job {} for {}", jobId, serviceContext);
            drmaaSession.deleteJobTemplate(jt);
            jt = null;
            JobInfo jobInfo = drmaaSession.wait(jobId, Session.TIMEOUT_WAIT_FOREVER);

            if (jobInfo.wasAborted()) {
                logger.error("Job {} for {} never ran", jobId, serviceContext);
                completableFuture.completeExceptionally(new ComputationException(serviceContext, String.format("Job %s never ran", jobId)));
            } else if (jobInfo.hasExited()) {
                logger.info("Job {} for {} completed with exist status {}", jobId, serviceContext, jobInfo.getExitStatus());
                if (jobInfo.getExitStatus() != 0) {
                    completableFuture.completeExceptionally(new ComputationException(serviceContext, String.format("Job %s completed with status %d", jobId, jobInfo.getExitStatus())));
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
        } catch (DrmaaException e) {
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
            try {
                drmaaSession.exit();
            } catch (DrmaaException e) {
                logger.warn("Error exiting DRMAA session for {} with {}", serviceContext, cmdArgs, e);
            }
        }
        return completableFuture;
    }
}
