package org.janelia.jacs2.service.impl;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.qualifier.ClusterJob;
import org.janelia.jacs2.service.qualifier.LocalJob;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public abstract class AbstractExternalProcessComputation<R> extends AbstractServiceComputation<R> {
    private static final String LOCAL_RUNNER = "local";
    private static final String CLUSTER_RUNNER = "cluster";

    @PropertyValue(name = "Executables.ModuleBase") @Inject
    private String executablesBaseDir;
    @Any @Inject
    private Instance<ExternalProcessRunner> serviceRunners;

    private ExternalProcessRunner getProcessRunner(String whichRunner) {
        String processRunner = StringUtils.defaultIfBlank(whichRunner, LOCAL_RUNNER);

        Class<? extends Annotation> annotationType;
        switch (processRunner) {
            case CLUSTER_RUNNER:
                annotationType = ClusterJob.class;
                break;
            case LOCAL_RUNNER:
            default:
                annotationType = LocalJob.class;
        }
        for (ExternalProcessRunner serviceRunner : serviceRunners) {
            if (serviceRunner.getClass().isAnnotationPresent(annotationType)) {
                return serviceRunner;
            }
        }
        throw new IllegalArgumentException("Unsupported runner: " + whichRunner);
    }

    protected abstract List<String> prepareCmdArgs(JacsServiceData jacsServiceData);
    protected abstract Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData);

    protected Optional<String> getEnvVar(String varName) {
        return Optional.ofNullable(System.getenv(varName));
    }

    protected String getFullExecutableName(String exeName) {
        StringBuilder cmdBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(executablesBaseDir)) {
            cmdBuilder.append(executablesBaseDir);
            if (cmdBuilder.charAt(cmdBuilder.length() - 1) != '/') {
                cmdBuilder.append('/');
            }
        }
        cmdBuilder.append(exeName);
        return cmdBuilder.toString();
    }

    protected String getUpdatedEnvValue(String varName, String addedValue) {
        Preconditions.checkArgument(StringUtils.isNotBlank(addedValue), "Cannot update environment variable " + varName + " with a null or empty value");
        Optional<String> currentValue = getEnvVar(varName);
        if (currentValue.isPresent()) {
            return currentValue.get() + ":" + addedValue;
        } else {
            return addedValue;
        }
    }

    @Override
    public CompletionStage<JacsService<R>> processData(JacsService<R> jacsService) {
        return getProcessRunner(jacsService.getJacsServiceData().getServiceType()).runCmd(
                jacsService.getJacsServiceData().getServiceCmd(),
                prepareCmdArgs(jacsService.getJacsServiceData()),
                prepareEnvironment(jacsService.getJacsServiceData()),
                jacsService);
    }
}
