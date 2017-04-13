package org.janelia.jacs2.asyncservice.common;

import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.asyncservice.qualifier.ClusterJob;
import org.janelia.jacs2.model.jacsservice.JacsServiceEventTypes;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.util.Collections;
import java.util.Map;

@ClusterJob
public class ExternalDrmaaJobRunner extends AbstractExternalProcessRunner {

    private final Session drmaaSession;

    @Inject
    public ExternalDrmaaJobRunner(Session drmaaSession, JacsServiceDataPersistence jacsServiceDataPersistence, Logger logger) {
        super(jacsServiceDataPersistence, logger);
        this.drmaaSession = drmaaSession;
    }

    @Override
    public ExeJobInfo runCmds(ExternalCodeBlock externalCode,
                              Map<String, String> env,
                              String workingDirName,
                              JacsServiceData serviceContext) {
        logger.debug("Begin DRMAA job invocation for {}", serviceContext);
        String processingScript = createProcessingScript(externalCode, workingDirName, serviceContext);
        serviceContext.setState(JacsServiceState.RUNNING);
        this.jacsServiceDataPersistence.update(serviceContext);
        JobTemplate jt = null;
        File outputFile;
        File errorFile;
        try {
            jt = drmaaSession.createJobTemplate();
            jt.setJobName(serviceContext.getName());
            jt.setRemoteCommand(processingScript);
            jt.setArgs(Collections.emptyList());
            File workingDirectory = setJobWorkingDirectory(jt, workingDirName);
            logger.debug("Using working directory {} for {}", workingDirectory, serviceContext);
            jt.setJobEnvironment(env);
            if (StringUtils.isNotBlank(serviceContext.getInputPath())) {
                jt.setInputPath(":" + serviceContext.getInputPath());
            }
            if (StringUtils.isNotBlank(serviceContext.getOutputPath())) {
                outputFile = new File(serviceContext.getOutputPath());
                Files.createParentDirs(outputFile);
            } else {
                throw new IllegalArgumentException("Output file must be set before running the service " + serviceContext.getName());
            }
            jt.setOutputPath(":" + outputFile.getAbsolutePath());
            if (StringUtils.isNotBlank(serviceContext.getErrorPath())) {
                errorFile = new File(serviceContext.getErrorPath());
                Files.createParentDirs(errorFile);
            } else {
                throw new IllegalArgumentException("Error file must be set before running the service " + serviceContext.getName());
            }
            jt.setErrorPath(":" + errorFile.getAbsolutePath());
            String nativeSpec = createNativeSpec(serviceContext);
            if (StringUtils.isNotBlank(nativeSpec)) {
                jt.setNativeSpecification(nativeSpec);
            }
            logger.debug("Start {} using {} with content={}; env={}", serviceContext, processingScript, externalCode, env);
            String jobId = drmaaSession.runJob(jt);
            logger.info("Submitted job {} for {}", jobId, serviceContext);
            serviceContext.addEvent(JacsServiceEventTypes.DRMAA_SUBMIT, String.format("Submitted job %s {%s} running: %s", serviceContext.getName(), jobId, processingScript));
            drmaaSession.deleteJobTemplate(jt);
            jt = null;
            return new DrmaaJobInfo(drmaaSession, jobId, processingScript);
        } catch (Exception e) {
            serviceContext.setState(JacsServiceState.ERROR);
            serviceContext.addEvent(JacsServiceEventTypes.DRMAA_JOB_ERROR, String.format("Error creating DRMAA job %s - %s", serviceContext.getName(), e.getMessage()));
            logger.error("Error creating a DRMAA job {} for {}", processingScript, serviceContext, e);
            throw new ComputationException(serviceContext, e);
        } finally {
            this.jacsServiceDataPersistence.update(serviceContext);
            if (jt != null) {
                try {
                    drmaaSession.deleteJobTemplate(jt);
                } catch (DrmaaException e) {
                    logger.error("Error deleting a DRMAA job {} for {}", processingScript, serviceContext, e);
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
        if (StringUtils.isNotBlank(jobResources.get("gridBatch"))) {
            nativeSpecBuilder.append("-pe batch ").append(jobResources.get("gridBatch")).append(' ');
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
