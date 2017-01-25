package org.janelia.jacs2.asyncservice.common;

import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.asyncservice.qualifier.ClusterJob;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@ClusterJob
public class ExternalDrmaaJobRunner implements ExternalProcessRunner {

    private final Session drmaaSession;
    private final Logger logger;

    @Inject
    public ExternalDrmaaJobRunner(Session drmaaSession, Logger logger) {
        this.drmaaSession = drmaaSession;
        this.logger = logger;
    }

    @Override
    public void runCmd(String cmd, List<String> cmdArgs, Map<String, String> env, String workingDirName,
                       ExternalProcessOutputHandler outStreamHandler, ExternalProcessOutputHandler errStreamHandler, JacsServiceData serviceContext) {
        logger.debug("Begin DRMAA job invocation for {}", serviceContext);
        JobTemplate jt = null;
        File outputFile = null;
        File errorFile = null;
        try {
            jt = drmaaSession.createJobTemplate();
            jt.setJobName(serviceContext.getName());
            jt.setRemoteCommand(cmd);
            jt.setArgs(cmdArgs);
            File workingDirectory = setJobWorkingDirectory(jt, workingDirName);
            logger.debug("Using working directory {} for {}", workingDirectory, serviceContext);
            jt.setJobEnvironment(env);
            if (StringUtils.isNotBlank(serviceContext.getInputPath())) {
                jt.setInputPath(":" + serviceContext.getInputPath());
            }
            if (StringUtils.isNotBlank(serviceContext.getOutputPath())) {
                outputFile = new File(serviceContext.getOutputPath());
                Files.createParentDirs(outputFile);
                jt.setOutputPath(":" + outputFile.getAbsolutePath());
            }
            if (StringUtils.isNotBlank(serviceContext.getErrorPath())) {
                errorFile = new File(serviceContext.getErrorPath());
                Files.createParentDirs(errorFile);
                jt.setErrorPath(":" + errorFile.getAbsolutePath());
            }
            String nativeSpec = createNativeSpec(serviceContext);
            if (StringUtils.isNotBlank(nativeSpec)) {
                jt.setNativeSpecification(nativeSpec);
            }
            String jobId = drmaaSession.runJob(jt);
            logger.info("Submitted job {} for {} with {} {}; env={}", jobId, serviceContext, cmd, cmdArgs, env);
            drmaaSession.deleteJobTemplate(jt);
            jt = null;
            if (outputFile == null) {
                outputFile = new File(workingDirectory, serviceContext.getName() + ".o" + jobId);
            }
            if (errorFile == null) {
                errorFile = new File(workingDirectory, serviceContext.getName() + ".e" + jobId);
            }
            JobInfo jobInfo = drmaaSession.wait(jobId, Session.TIMEOUT_WAIT_FOREVER);

            if (jobInfo.wasAborted()) {
                logger.error("Job {} for {} never ran", jobId, serviceContext);
                throw new ComputationException(serviceContext, String.format("Job %s never ran", jobId));
            } else if (jobInfo.hasExited()) {
                logger.info("Job {} for {} completed with exist status {}", jobId, serviceContext, jobInfo.getExitStatus());
                ExternalProcessIOHandler processStdoutHandler = null;
                try (InputStream outputStream = new FileInputStream(outputFile)) {
                    processStdoutHandler = new ExternalProcessIOHandler(outStreamHandler, outputStream);
                    processStdoutHandler.run();
                }
                ExternalProcessIOHandler processStderrHandler = null;
                try (InputStream errorStream = new FileInputStream(errorFile)) {
                    processStderrHandler = new ExternalProcessIOHandler(errStreamHandler, errorStream);
                    processStderrHandler.run();
                }
                if (jobInfo.getExitStatus() != 0) {
                    throw new ComputationException(serviceContext, String.format("Job %s completed with status %d", jobId, jobInfo.getExitStatus()));
                } else if (processStdoutHandler.getResult() != null) {
                    throw new ComputationException(serviceContext, "Process error: " + processStdoutHandler.getResult());
                } else if (processStderrHandler.getResult() != null) {
                    throw new ComputationException(serviceContext, "Process error: " + processStderrHandler.getResult());
                }
            } else if (jobInfo.hasSignaled()) {
                logger.warn("Job {} for {} terminated due to signal {}", jobId, serviceContext, jobInfo.getTerminatingSignal());
                throw new ComputationException(serviceContext, String.format("Job %s completed with status %s", jobId, jobInfo.getTerminatingSignal()));
            } else {
                logger.warn("Job {} for {} finished with unclear conditions", jobId, serviceContext);
                throw new ComputationException(serviceContext, String.format("Job %s completed with unclear conditions", jobId));
            }
        } catch (Exception e) {
            logger.error("Error running a DRMAA job for {} with {}", serviceContext, cmdArgs, e);
            throw new ComputationException(serviceContext, e);
        } finally {
            if (jt != null) {
                try {
                    drmaaSession.deleteJobTemplate(jt);
                } catch (DrmaaException e) {
                    logger.warn("Error deleting the DRMAA job template for {} with {}", serviceContext, cmdArgs, e);
                }
            }
        }
    }

    private File setJobWorkingDirectory(JobTemplate jt, String workingDirName) {
        File workingDirectory;
        if (StringUtils.isNotBlank(workingDirName)) {
            workingDirectory = new File(workingDirName);
        } else {
            workingDirectory = Files.createTempDir();
        }
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }
        if (!workingDirectory.exists()) {
            throw new IllegalStateException("Cannot create working directory " + workingDirectory.getAbsolutePath());
        }
        try {
            jt.setWorkingDirectory(workingDirectory.getAbsolutePath());
        } catch (DrmaaException e) {
            throw new IllegalStateException(e);
        }
        return workingDirectory;
    }

    private String createNativeSpec(JacsServiceData serviceContext) {
        StringBuilder nativeSpecBuilder = new StringBuilder();
        Map<String, String> jobResources = serviceContext.getResources();
        // append accountID
        if (StringUtils.isNotBlank(jobResources.get("gridAccountId"))) {
            nativeSpecBuilder.append("-A ").append(jobResources.get("gridAccountId")).append(' ');
        }
        // append processing environment
        if (StringUtils.isNotBlank(jobResources.get("gridPE"))) {
            nativeSpecBuilder.append("-pe ").append(jobResources.get("gridPE")).append(' ');
        }
        // append grid queue
        if (StringUtils.isNotBlank(jobResources.get("gridQueue"))) {
            nativeSpecBuilder.append("-q ").append(jobResources.get("gridQueue")).append(' ');
        }
        // append grid resource limits - the resource limits must be specified as a comma delimited list of <name>'='<value>, e.g.
        // gridResourceLimits: "short=true,scalityr=1,scalityw=1,haswell=true"
        if (StringUtils.isNotBlank(jobResources.get("gridResourceLimits"))) {
            nativeSpecBuilder.append("-l ").append(jobResources.get("gridResourceLimits")).append(' ');
        }
        return nativeSpecBuilder.toString();
    }

}
