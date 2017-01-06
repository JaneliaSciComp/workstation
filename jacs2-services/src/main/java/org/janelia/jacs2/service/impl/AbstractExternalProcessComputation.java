package org.janelia.jacs2.service.impl;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.ProcessingLocation;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public abstract class AbstractExternalProcessComputation<R> extends AbstractServiceComputation<R> {

    protected static final String DY_LIBRARY_PATH_VARNAME = "LD_LIBRARY_PATH";

    @Inject
    private Logger logger;
    @PropertyValue(name = "Executables.ModuleBase")
    @Inject
    private String executablesBaseDir;
    @PropertyValue(name = "service.DefaultWorkingDir")
    @Inject
    private String defaultWorkingDir;
    @Any @Inject
    private Instance<ExternalProcessRunner> serviceRunners;

    private ExternalProcessRunner getProcessRunner(ProcessingLocation processingLocation) {
        ProcessingLocation location = processingLocation == null ? ProcessingLocation.LOCAL : processingLocation;
        for (ExternalProcessRunner serviceRunner : serviceRunners) {
            if (serviceRunner.getClass().isAnnotationPresent(location.getProcessingAnnotationClass())) {
                return serviceRunner;
            }
        }
        throw new IllegalArgumentException("Unsupported runner: " + processingLocation);
    }

    protected abstract List<String> prepareCmdArgs(JacsService<R> jacsService);
    protected abstract Map<String, String> prepareEnvironment(JacsService<R> jacsServiceData);

    protected String getWorkingDirectory(JacsService<R> jacsService, String workingDir) {
        if (StringUtils.isNotBlank(workingDir)) {
            return workingDir;
        } else if (StringUtils.isNotBlank(jacsService.getWorkspace())) {
            return jacsService.getWorkspace();
        } else if (StringUtils.isNotBlank(defaultWorkingDir)) {
            return defaultWorkingDir;
        } else {
            return System.getProperty("java.io.tmpdir");
        }
    }

    protected Optional<String> getEnvVar(String varName) {
        return Optional.ofNullable(System.getenv(varName));
    }

    protected String getFullExecutableName(String... execPathComponents) {
        Path cmdPath;
        if (StringUtils.isNotBlank(executablesBaseDir)) {
            cmdPath = Paths.get(executablesBaseDir, execPathComponents);
        } else {
            cmdPath = Paths.get("", execPathComponents);
        }
        return cmdPath.toString();
    }

    protected String getUpdatedEnvValue(String varName, String addedValue) {
        Preconditions.checkArgument(StringUtils.isNotBlank(addedValue), "Cannot update environment variable " + varName + " with a null or empty value");
        Optional<String> currentValue = getEnvVar(varName);
        if (currentValue.isPresent()) {
            return currentValue.get().endsWith(":")  ? currentValue.get() + addedValue : currentValue.get() + ":" + addedValue;
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
                if (checkForErrors(l)) {
                    error = l;
                }
            } catch (IOException re) {
                logger.error("Error reading process output", re);
                return "Error reading process output";
            }
        }
        return error;
    }

    protected boolean checkForErrors(String l) {
        if (StringUtils.isNotBlank(l) && l.matches("(?i:.*(error|exception).*)")) {
            logger.error(l);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public CompletionStage<JacsService<R>> processData(JacsService<R> jacsService) {
        List<String> args = prepareCmdArgs(jacsService);
        Map<String, String> env = prepareEnvironment(jacsService);
        return getProcessRunner(jacsService.getProcessingLocation()).runCmd(
                jacsService.getServiceCmd(),
                args,
                env,
                getWorkingDirectory(jacsService, defaultWorkingDir),
                this::outputStreamHandler,
                this::errStreamHandler,
                jacsService);
    }
}
