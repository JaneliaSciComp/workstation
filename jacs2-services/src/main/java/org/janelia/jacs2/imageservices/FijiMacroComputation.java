package org.janelia.jacs2.imageservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.service.impl.AbstractExternalProcessComputation;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.JacsService;
import org.janelia.jacs2.utils.ScriptingUtils;
import org.slf4j.Logger;
import sun.font.Script;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Named("basicMIPAndMovieGenerationService")
public class FijiMacroComputation extends AbstractExternalProcessComputation<File> {

    protected static final int START_DISPLAY_PORT = 890;
    protected static final int TIMEOUT_SECONDS = 3600;  // 60 minutes

    private final String fijiExecutable;
    private final String fijiMacrosPath;
    private final String libraryPath;
    private final String defaultWorkingDir;
    private final Logger logger;

    @Inject
    public FijiMacroComputation(@PropertyValue(name = "Fiji.Bin.Path") String fijiExecutable,
                                @PropertyValue(name = "Fiji.Macro.Path") String fijiMacrosPath,
                                @PropertyValue(name = "VAA3D.LibraryPath") String libraryPath,
                                @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                Logger logger) {
        this.fijiExecutable = fijiExecutable;
        this.fijiMacrosPath = fijiMacrosPath;
        this.libraryPath = libraryPath;
        this.defaultWorkingDir = defaultWorkingDir;
        this.logger = logger;
    }

    @Override
    public CompletionStage<JacsService<File>> preProcessData(JacsService<File> jacsService) {
        CompletableFuture<JacsService<File>> preProcess = new CompletableFuture<>();
        FijiMacroServiceDescriptor.FijiMacroArgs args = getArgs(jacsService);
        if (StringUtils.isBlank(args.macroName)) {
            preProcess.completeExceptionally(new ComputationException(jacsService, "FIJI macro must be specified"));
        } else {
            preProcess.complete(jacsService);
        }
        return preProcess;
    }

    @Override
    protected List<String> prepareCmdArgs(JacsService<File> jacsService) {
        FijiMacroServiceDescriptor.FijiMacroArgs args = getArgs(jacsService);
        File scriptFile = createScript(jacsService, args);
        jacsService.setServiceCmd(scriptFile.getAbsolutePath());
        return ImmutableList.of();
    }

    private File createScript(JacsService<File> jacsService, FijiMacroServiceDescriptor.FijiMacroArgs args) {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwx------");
        BufferedWriter scriptStream = null;
        File scriptFile = null;
        try {
            scriptFile = Files.createFile(
                    Paths.get(args.workingDir, jacsService.getName() + "_" + jacsService.getId() + ".sh"),
                    PosixFilePermissions.asFileAttribute(perms)).toFile();
            scriptStream = new BufferedWriter(new FileWriter(scriptFile));
            scriptStream.append("DISPLAY_PORT=").append(Integer.toString(ScriptingUtils.getRandomPort(START_DISPLAY_PORT))).append('\n');
            scriptStream.append(ScriptingUtils.startScreenCapture("$DISPLAY_PORT", "1280x1024x24")).append('\n');
            // Create temp dir so that large temporary avis are not created on the network drive
            scriptStream.append("export TMPDIR=").append(args.workingDir).append("\n");
            scriptStream.append("mkdir -p $TMPDIR\n");
            scriptStream.append("TEMP_DIR=`mktemp -d`\n");
            scriptStream.append("function cleanTemp {\n");
            scriptStream.append("    rm -rf $TEMP_DIR\n");
            scriptStream.append("    echo \"Cleaned up $TEMP_DIR\"\n");
            scriptStream.append("}\n");

            // Two EXIT handlers
            scriptStream.append("function exitHandler() { cleanXvfb; cleanTemp; }\n");
            scriptStream.append("trap exitHandler EXIT\n");

            String fijiCmd = String.format("%s -macro %s %s\n", getFijiExecutable(args), getFullFijiMacro(args), args.macroArgs);
            scriptStream.append(fijiCmd).append('\n');
            // Monitor Fiji and take periodic screenshots, killing it eventually
            scriptStream.append("fpid=$!\n");
            scriptStream.append(ScriptingUtils.screenCaptureLoop("./xvfb.${PORT}", "PORT", "fpid", 30, TIMEOUT_SECONDS));

            scriptStream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (scriptStream != null) {
                try {
                    scriptStream.close();
                } catch (IOException ignore) {
                }
            }
        }
        return scriptFile;
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsService<File> jacsService) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    private FijiMacroServiceDescriptor.FijiMacroArgs getArgs(JacsService<File> jacsService) {
        FijiMacroServiceDescriptor.FijiMacroArgs args = new FijiMacroServiceDescriptor.FijiMacroArgs();
        new JCommander(args).parse(jacsService.getArgsArray());
        if (StringUtils.isNotBlank(jacsService.getWorkspace())) {
            args.workingDir = jacsService.getWorkspace();
        } else if (StringUtils.isNotBlank(defaultWorkingDir)) {
            args.workingDir = defaultWorkingDir;
        } else {
            args.workingDir = System.getProperty("java.io.tmpdir");
        }
        return args;
    }

    private String getFijiExecutable(FijiMacroServiceDescriptor.FijiMacroArgs args) {
        return getFullExecutableName(fijiExecutable);
    }

    private String getFullFijiMacro(FijiMacroServiceDescriptor.FijiMacroArgs args) {
        if (args.macroName.startsWith("/")) {
            return args.macroName;
        } else {
            return getFullExecutableName(fijiMacrosPath, args.macroName);
        }
    }

    @Override
    public void postProcessData(JacsService<File> jacsService, Throwable exc) {
        if (exc == null) {
            try {
                logger.debug("Delete temporary service script: {}", jacsService.getServiceCmd());
                Files.deleteIfExists(new File(jacsService.getJacsServiceData().getServiceCmd()).toPath());
            } catch (IOException e) {
                logger.error("Error deleting the service script {}", jacsService.getServiceCmd(), e);
            }
        }
    }

}
