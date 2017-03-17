package org.janelia.jacs2.asyncservice.common;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractExeBasedServiceProcessor<T> extends AbstractBasicLifeCycleServiceProcessor<T> implements ServiceCommand {

    protected static final String DY_LIBRARY_PATH_VARNAME = "LD_LIBRARY_PATH";

    private final String executablesBaseDir;
    private final Instance<ExternalProcessRunner> serviceRunners;

    public AbstractExeBasedServiceProcessor(JacsServiceEngine jacsServiceEngine,
                                            ServiceComputationFactory computationFactory,
                                            JacsServiceDataPersistence jacsServiceDataPersistence,
                                            String defaultWorkingDir,
                                            String executablesBaseDir,
                                            Instance<ExternalProcessRunner> serviceRunners,
                                            Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.serviceRunners = serviceRunners;
        this.executablesBaseDir = executablesBaseDir;
    }

    @Override
    public void execute(JacsServiceData jacsServiceData) {
        execute(this::runExternalProcess, jacsServiceData);
    }

    @Override
    protected ServiceComputation<T> processing(JacsServiceData jacsServiceData) {
        return invokeExternalProcess(jacsServiceData)
                .thenApply(this::waitForResult);
    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        return ImmutableList.of();
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

    protected String outputStreamHandler(InputStream outStream) {
        return streamHandler(outStream, s -> {if (StringUtils.isNotBlank(s)) logger.debug(s);});
    }

    protected String errStreamHandler(InputStream outStream) {
        return streamHandler(outStream, s -> {if (StringUtils.isNotBlank(s)) logger.error(s);});
    }

    private String streamHandler(InputStream outStream, Consumer<String> logWriter) {
        BufferedReader outputReader = new BufferedReader(new InputStreamReader(outStream));
        String error = null;
        for (; ; ) {
            try {
                String l = outputReader.readLine();
                if (l == null) break;
                logWriter.accept(l);
                if (hasErrors(l)) {
                    error = l;
                }
            } catch (IOException re) {
                logger.error("Error reading process output", re);
                return "Error reading process output";
            }
        }
        return error;
    }

    protected boolean hasErrors(String l) {
        if (StringUtils.isNotBlank(l) && l.matches("(?i:.*(error|exception).*)")) {
            logger.error(l);
            return true;
        } else {
            return false;
        }
    }

    protected ServiceComputation<JacsServiceData> invokeExternalProcess(JacsServiceData jacsServiceData) {
        return computationFactory.<JacsServiceData>newComputation()
                .supply(() -> {
                    runExternalProcess(jacsServiceData);
                    return jacsServiceData;
                });
    }

    protected void runExternalProcess(JacsServiceData jacsServiceData) {
        ExternalCodeBlock script = prepareExternalScript(jacsServiceData);
        Map<String, String> env = prepareEnvironment(jacsServiceData);
        getProcessRunner(jacsServiceData.getProcessingLocation()).runCmds(
                script,
                env,
                getWorkingDirectory(jacsServiceData).toString(),
                this::outputStreamHandler,
                this::errStreamHandler,
                jacsServiceData);
        success(jacsServiceData, Optional.ofNullable(retrieveResult(jacsServiceData)));
    }

    private ExternalProcessRunner getProcessRunner(ProcessingLocation processingLocation) {
        ProcessingLocation location = processingLocation == null ? ProcessingLocation.LOCAL : processingLocation;
        for (ExternalProcessRunner serviceRunner : serviceRunners) {
            if (serviceRunner.getClass().isAnnotationPresent(location.getProcessingAnnotationClass())) {
                return serviceRunner;
            }
        }
        throw new IllegalArgumentException("Unsupported runner: " + processingLocation);
    }

}
