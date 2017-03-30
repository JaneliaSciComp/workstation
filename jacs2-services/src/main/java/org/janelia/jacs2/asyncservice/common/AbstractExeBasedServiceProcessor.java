package org.janelia.jacs2.asyncservice.common;

import com.google.common.base.Preconditions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceEventTypes;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractExeBasedServiceProcessor<T> extends AbstractBasicLifeCycleServiceProcessor<T> {

    protected static final String DY_LIBRARY_PATH_VARNAME = "LD_LIBRARY_PATH";

    private final String executablesBaseDir;
    private final Instance<ExternalProcessRunner> serviceRunners;

    public AbstractExeBasedServiceProcessor(ServiceComputationFactory computationFactory,
                                            JacsServiceDataPersistence jacsServiceDataPersistence,
                                            Instance<ExternalProcessRunner> serviceRunners,
                                            String defaultWorkingDir,
                                            String executablesBaseDir,
                                            Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.serviceRunners = serviceRunners;
        this.executablesBaseDir = executablesBaseDir;
    }

    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        ExeJobInfo jobInfo = runExternalProcess(jacsServiceData);
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenSuspendUntil(() -> this.hasJobFinished(jacsServiceData, jobInfo))
                .thenApply(sd -> {
                    List<String> errors = this.getErrorChecker().collectErrors(jacsServiceData);
                    String errorMessage = null;
                    if (CollectionUtils.isNotEmpty(errors)) {
                        errorMessage = String.format("Process %s failed; errors found: %s", jobInfo.getScriptName(), String.join(";", errors));
                    } else if (jobInfo.hasFailed()) {
                        errorMessage = String.format("Process %s failed", jobInfo.getScriptName());
                    }
                    if (errorMessage != null) {
                        jacsServiceData.addEvent(JacsServiceEventTypes.FAILED, errorMessage);
                        jacsServiceData.setState(JacsServiceState.ERROR);
                        updateServiceData(jacsServiceData);
                        throw new ComputationException(jacsServiceData, errorMessage);
                    }
                    return sd;
                });
    }

    protected boolean hasJobFinished(JacsServiceData jacsServiceData, ExeJobInfo jobInfo) {
        if (jobInfo.isDone()) {
            return true;
        }
        try {
            verifyTimeOut(jacsServiceData);
        } catch (ComputationException e) {
            jobInfo.terminate();
            throw e;
        }
        return false;
    }

    protected abstract ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData);

    protected abstract Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData);

    protected Optional<String> getEnvVar(String varName) {
        return Optional.ofNullable(System.getenv(varName));
    }

    protected String getFullExecutableName(String... execPathComponents) {
        String baseDir;
        String[] pathComponents;
        if (execPathComponents.length > 0 && StringUtils.startsWith(execPathComponents[0], "/")) {
            baseDir = execPathComponents[0];
            pathComponents = Arrays.copyOfRange(execPathComponents, 1, execPathComponents.length);
        } else {
            baseDir = executablesBaseDir;
            pathComponents = execPathComponents;
        }
        Path cmdPath;
        if (StringUtils.isNotBlank(baseDir)) {
            cmdPath = Paths.get(baseDir, pathComponents);
        } else {
            cmdPath = Paths.get("", execPathComponents);
        }
        return cmdPath.toString();
    }

    protected String getUpdatedEnvValue(String varName, String addedValue) {
        Preconditions.checkArgument(StringUtils.isNotBlank(addedValue), "Cannot update environment variable " + varName + " with a null or empty value");
        Optional<String> currentValue = getEnvVar(varName);
        if (currentValue.isPresent()) {
            // prepend the new value
            return addedValue + ":" + currentValue.get();
        } else {
            return addedValue;
        }
    }

    protected ExeJobInfo runExternalProcess(JacsServiceData jacsServiceData) {
        ExternalCodeBlock script = prepareExternalScript(jacsServiceData);
        Map<String, String> env = prepareEnvironment(jacsServiceData);
        return getProcessRunner(jacsServiceData.getProcessingLocation()).runCmds(
                script,
                env,
                getWorkingDirectory(jacsServiceData).toString(),
                jacsServiceData);
    }

    private ExternalProcessRunner getProcessRunner(ProcessingLocation processingLocation) {
        ProcessingLocation location = processingLocation == null ? ProcessingLocation.LOCAL : processingLocation;
        for (ExternalProcessRunner serviceRunner : serviceRunners) {
            if (serviceRunner.supports(location)) {
                return serviceRunner;
            }
        }
        throw new IllegalArgumentException("Unsupported runner: " + processingLocation);
    }

}
