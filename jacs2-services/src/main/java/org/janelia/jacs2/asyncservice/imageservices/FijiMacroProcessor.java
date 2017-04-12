package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.DefaultServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.VoidServiceResultHandler;
import org.janelia.jacs2.asyncservice.utils.ScriptUtils;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.asyncservice.utils.X11Utils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Named("fijiMacro")
public class FijiMacroProcessor extends AbstractExeBasedServiceProcessor<Void> {

    static class FijiMacroArgs extends ServiceArgs {
        @Parameter(names = "-macro", description = "FIJI macro name", required = true)
        String macroName;
        @Parameter(names = "-macroArgs", description = "Arguments for the fiji macro")
        List<String> macroArgs;
        @Parameter(names = "-temporaryOutput", description = "Temporary output directory")
        String temporaryOutput;
        @Parameter(names = "-finalOutput", description = "Final output directory")
        String finalOutput;
        @Parameter(names = "-resultsPatterns", description = "results patterns")
        List<String> resultsPatterns = new ArrayList<>();
    }

    private final String fijiExecutable;
    private final String fijiMacrosPath;

    @Inject
    FijiMacroProcessor(ServiceComputationFactory computationFactory,
                       JacsServiceDataPersistence jacsServiceDataPersistence,
                       @Any Instance<ExternalProcessRunner> serviceRunners,
                       @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                       @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                       @PropertyValue(name = "Fiji.Bin.Path") String fijiExecutable,
                       @PropertyValue(name = "Fiji.Macro.Path") String fijiMacrosPath,
                       Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, serviceRunners, defaultWorkingDir, executablesBaseDir, logger);
        this.fijiExecutable = fijiExecutable;
        this.fijiMacrosPath = fijiMacrosPath;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new FijiMacroArgs());
    }

    @Override
    public ServiceResultHandler<Void> getResultHandler() {
        return new VoidServiceResultHandler();
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return new DefaultServiceErrorChecker(logger) {
            @Override
            protected boolean hasErrors(String l) {
                if (StringUtils.isNotBlank(l) && l.matches("(?i:.*(error|exception).*)")) {
                    if (l.contains("Cannot write XdndAware property") ||
                            l.contains("java.rmi.ConnectException") ||
                            l.contains("java.net.ConnectException")) {
                        logger.warn(l);
                        return false;
                    }
                    logger.error(l);
                    return true;
                } else {
                    return false;
                }
            }
        };
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            FijiMacroArgs args = getArgs(jacsServiceData);
            Path temporaryOutput = getTemporaryDir(args);
            if (temporaryOutput != null) {
                Files.createDirectories(temporaryOutput);
            }
        } catch (Exception e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        FijiMacroArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        createScript(jacsServiceData, args, externalScriptWriter);
        externalScriptWriter.close();
        return externalScriptCode;
    }

    private void createScript(JacsServiceData jacsServiceData, FijiMacroArgs args, ScriptWriter scriptWriter) {
        try {
            if (StringUtils.isNotBlank(args.temporaryOutput)) {
                Files.createDirectories(Paths.get(args.temporaryOutput));
            }
            if (StringUtils.isNotBlank(args.finalOutput)) {
                Files.createDirectories(Paths.get(args.finalOutput));
            }
            Path workingDir = getWorkingDirectory(jacsServiceData);
            X11Utils.setDisplayPort(workingDir.toString(), scriptWriter);
            // Create temp dir so that large temporary avis are not created on the network drive
            String scratchFolder = StringUtils.defaultIfBlank(args.temporaryOutput, workingDir.toString());
            Path scratchDir = Paths.get(scratchFolder, jacsServiceData.getName(), jacsServiceData.getName() + "_" + jacsServiceData.getId());
            Files.createDirectories(scratchDir);
            ScriptUtils.createTempDir("cleanTemp", scratchDir.toString(), scriptWriter);
            // define the exit handlers
            scriptWriter
                    .add("function exitHandler() { cleanXvfb; cleanTemp; }")
                    .add("trap exitHandler EXIT\n");

            scriptWriter.addBackground(String.format("%s -macro %s %s", getFijiExecutable(), getFullFijiMacro(args), String.join(",", args.macroArgs)));
            // Monitor Fiji and take periodic screenshots, killing it eventually
            scriptWriter.setVar("fpid", "$!");
            X11Utils.startScreenCaptureLoop(scratchDir + "/xvfb-" + jacsServiceData.getId() + ".${PORT}",
                    "PORT", "fpid", 30, getTimeoutInSeconds(jacsServiceData), scriptWriter);
            if (StringUtils.isNotBlank(args.finalOutput) && StringUtils.isNotBlank(args.temporaryOutput) &&
                    !args.finalOutput.equals(args.temporaryOutput)) {
                // the copy should not fail if the file exists
                if (args.resultsPatterns.isEmpty()) {
                    scriptWriter.add(String.format("cp -a %s/* %s || true", args.temporaryOutput, args.finalOutput));
                } else {
                    args.resultsPatterns.forEach(resultPattern -> scriptWriter.add(String.format("cp %s/%s %s || true", args.temporaryOutput, resultPattern, args.finalOutput)));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        return ImmutableMap.of();
    }

    private FijiMacroArgs getArgs(JacsServiceData jacsServiceData) {
        FijiMacroArgs args = new FijiMacroArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private String getFijiExecutable() {
        return getFullExecutableName(fijiExecutable);
    }

    private String getFullFijiMacro(FijiMacroArgs args) {
        if (args.macroName.startsWith("/")) {
            return args.macroName;
        } else {
            return getFullExecutableName(fijiMacrosPath, args.macroName);
        }
    }

    private int getTimeoutInSeconds(JacsServiceData sd) {
        long timeoutInMillis = sd.timeout();
        if (timeoutInMillis > 0) {
            return (int) timeoutInMillis / 1000;
        } else {
            return X11Utils.DEFAULT_TIMEOUT_SECONDS;
        }
    }

    private Path getTemporaryDir(FijiMacroArgs args) {
        if (StringUtils.isNotBlank(args.temporaryOutput)) {
            return Paths.get(args.temporaryOutput);
        } else {
            return null;
        }
    }
}
