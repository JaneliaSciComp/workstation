package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.utils.ScriptUtils;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.asyncservice.utils.X11Utils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.JacsServiceDispatcher;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class FijiMacroProcessor extends AbstractExeBasedServiceProcessor<Void> {

    private final String fijiExecutable;
    private final String fijiMacrosPath;
    private final String libraryPath;

    @Inject
    FijiMacroProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                       ServiceComputationFactory computationFactory,
                       JacsServiceDataPersistence jacsServiceDataPersistence,
                       @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                       @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                       @Any Instance<ExternalProcessRunner> serviceRunners,
                       @PropertyValue(name = "Fiji.Bin.Path") String fijiExecutable,
                       @PropertyValue(name = "Fiji.Macro.Path") String fijiMacrosPath,
                       @PropertyValue(name = "VAA3D.LibraryPath") String libraryPath,
                       Logger logger) {
        super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.fijiExecutable = fijiExecutable;
        this.fijiMacrosPath = fijiMacrosPath;
        this.libraryPath = libraryPath;
    }

    @Override
    public Void getResult(JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public void setResult(Void result, JacsServiceData jacsServiceData) {
    }

    @Override
    protected ServiceComputation<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        FijiMacroServiceDescriptor.FijiMacroArgs args = getArgs(jacsServiceData);
        if (StringUtils.isBlank(args.macroName)) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "FIJI macro must be specified"));
        } else {
            return computationFactory.newCompletedComputation(jacsServiceData);
        }
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return true;
    }

    @Override
    protected Void retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        FijiMacroServiceDescriptor.FijiMacroArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        createScript(jacsServiceData, args, externalScriptWriter);
        externalScriptWriter.close();
        return externalScriptCode;
    }

    private void createScript(JacsServiceData jacsServiceData, FijiMacroServiceDescriptor.FijiMacroArgs args,
                              ScriptWriter scriptWriter) {
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

            scriptWriter.addBackground(String.format("%s -macro %s %s", getFijiExecutable(), getFullFijiMacro(args), args.macroArgs));
            // Monitor Fiji and take periodic screenshots, killing it eventually
            scriptWriter.setVar("fpid","$!");
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
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    private FijiMacroServiceDescriptor.FijiMacroArgs getArgs(JacsServiceData jacsServiceData) {
        FijiMacroServiceDescriptor.FijiMacroArgs args = new FijiMacroServiceDescriptor.FijiMacroArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private String getFijiExecutable() {
        return getFullExecutableName(fijiExecutable);
    }

    private String getFullFijiMacro(FijiMacroServiceDescriptor.FijiMacroArgs args) {
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
}
