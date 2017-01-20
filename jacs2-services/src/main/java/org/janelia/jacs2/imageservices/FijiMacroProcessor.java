package org.janelia.jacs2.imageservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.service.impl.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.ExternalProcessRunner;
import org.janelia.jacs2.service.impl.JacsServiceDispatcher;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceComputationFactory;
import org.janelia.jacs2.utils.ScriptingUtils;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FijiMacroProcessor extends AbstractExeBasedServiceProcessor<Void> {

    protected static final int START_DISPLAY_PORT = 890;
    protected static final int TIMEOUT_SECONDS = 3600;  // 60 minutes

    private final String fijiExecutable;
    private final String fijiMacrosPath;
    private final String libraryPath;
    private final String scratchLocation;

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
                       @PropertyValue(name = "service.DefaultScratchDir") String scratchLocation,
                       Logger logger) {
        super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.fijiExecutable = fijiExecutable;
        this.fijiMacrosPath = fijiMacrosPath;
        this.libraryPath = libraryPath;
        this.scratchLocation = scratchLocation;
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
    protected ServiceComputation<Void> postProcessData(Void processingResult, JacsServiceData jacsServiceData) {
        return computationFactory.<Void>newComputation()
                .supply(() -> {
                    try {
                        FijiMacroServiceDescriptor.FijiMacroArgs args = getArgs(jacsServiceData);
                        logger.debug("Delete temporary service script: {}", jacsServiceData.getServiceCmd());
                        Files.deleteIfExists(new File(jacsServiceData.getServiceCmd()).toPath());
                        return processingResult;
                    } catch (Exception e) {
                        throw new ComputationException(jacsServiceData, e);
                    }
                });
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
    protected List<String> prepareCmdArgs(JacsServiceData jacsServiceData) {
        FijiMacroServiceDescriptor.FijiMacroArgs args = getArgs(jacsServiceData);
        File scriptFile = createScript(jacsServiceData, args);
        jacsServiceData.setServiceCmd(scriptFile.getAbsolutePath());
        return ImmutableList.of();
    }

    private File createScript(JacsServiceData jacsServiceData, FijiMacroServiceDescriptor.FijiMacroArgs args) {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwx------");
        BufferedWriter scriptStream = null;
        File scriptFile = null;
        try {
            Path scratchDir = Paths.get(scratchLocation, jacsServiceData.getName(), jacsServiceData.getName() + "_" + jacsServiceData.getId());
            Path workingDir = getWorkingDirectory(jacsServiceData);
            Files.createDirectories(workingDir);
            scriptFile = Files.createFile(
                    Paths.get(workingDir.toString(), jacsServiceData.getName() + "_" + jacsServiceData.getId() + ".sh"),
                    PosixFilePermissions.asFileAttribute(perms)).toFile();
            scriptStream = new BufferedWriter(new FileWriter(scriptFile));
            scriptStream.append("DISPLAY_PORT=").append(Integer.toString(ScriptingUtils.getRandomPort(START_DISPLAY_PORT))).append('\n');
            scriptStream.append(ScriptingUtils.startScreenCapture("$DISPLAY_PORT", "1280x1024x24")).append('\n');
            // Create temp dir so that large temporary avis are not created on the network drive
            scriptStream.append("export TMPDIR=").append(scratchDir.toString()).append("\n");
            scriptStream.append("mkdir -p $TMPDIR\n");
            scriptStream.append("TEMP_DIR=`mktemp -d`\n");
            scriptStream.append("function cleanTemp {\n");
            scriptStream.append("    rm -rf $TEMP_DIR\n");
            scriptStream.append("    echo \"Cleaned up $TEMP_DIR\"\n");
            scriptStream.append("}\n");

            // Two EXIT handlers
            scriptStream.append("function exitHandler() { cleanXvfb; cleanTemp; }\n");
            scriptStream.append("trap exitHandler EXIT\n");

            String fijiCmd = String.format("%s -macro %s %s", getFijiExecutable(), getFullFijiMacro(args), args.macroArgs);
            scriptStream.append(fijiCmd).append('&').append('\n');
            // Monitor Fiji and take periodic screenshots, killing it eventually
            scriptStream.append("fpid=$!\n");
            scriptStream.append(ScriptingUtils.screenCaptureLoop(scratchDir + "/xvfb-" + jacsServiceData.getId() + ".${PORT}", "PORT", "fpid", 30, TIMEOUT_SECONDS));

            scriptStream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (scriptStream != null) {
                try {
                    scriptStream.close();
                } catch (IOException ignore) {
                    logger.warn("Error closing the FIJI script stream");
                }
            }
        }
        return scriptFile;
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
}
