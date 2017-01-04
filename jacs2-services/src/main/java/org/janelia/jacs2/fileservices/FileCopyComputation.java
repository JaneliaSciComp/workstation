package org.janelia.jacs2.fileservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.service.impl.AbstractExternalProcessComputation;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.JacsService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Named("fileCopyService")
public class FileCopyComputation extends AbstractExternalProcessComputation<File> {

    private static final String DY_LIBRARY_PATH_VARNAME = "LD_LIBRARY_PATH";

    private final Logger logger;
    private final String libraryPath;
    private final String scriptName;

    @Inject
    FileCopyComputation(@PropertyValue(name = "VAA3D.LibraryPath") String libraryPath, @PropertyValue(name = "Convert.ScriptPath") String scriptName, Logger logger) {
        this.libraryPath = libraryPath;
        this.scriptName = scriptName;
        this.logger = logger;
    }

    @Override
    public CompletionStage<JacsService<File>> preProcessData(JacsService<File> jacsService) {
        CompletableFuture<JacsService<File>> preProcess = new CompletableFuture<>();
        try {
            FileCopyServiceDescriptor.FileCopyArgs fileCopyArgs = getArgs(jacsService);
            if (StringUtils.isBlank(fileCopyArgs.sourceFilename)) {
                preProcess.completeExceptionally(new ComputationException(jacsService, "Source file name must be specified"));
            } else if (StringUtils.isBlank(fileCopyArgs.targetFilename)) {
                preProcess.completeExceptionally(new ComputationException(jacsService, "Target file name must be specified"));
            } else {
                File targetFile = new File(fileCopyArgs.targetFilename);
                Files.createDirectories(targetFile.getParentFile().toPath());
                jacsService.setResult(targetFile);
                preProcess.complete(jacsService);
            }
        } catch (Exception e) {
            logger.error("FileCopy preprocess error", e);
            preProcess.completeExceptionally(new ComputationException(jacsService, e));
        }
        return preProcess;
    }

    @Override
    public CompletionStage<JacsService<File>> isDone(JacsService<File> jacsService) {
        CompletableFuture<JacsService<File>> doneFuture = new CompletableFuture<>();
        FileCopyServiceDescriptor.FileCopyArgs fileCopyArgs = getArgs(jacsService);
        if (fileCopyArgs.deleteSourceFile) {
            try {
                File sourceFile = new File(fileCopyArgs.sourceFilename);
                Files.deleteIfExists(sourceFile.toPath());
            } catch (IOException e) {
                doneFuture.completeExceptionally(e);
            }
        } else {
            doneFuture.complete(jacsService);
        }
        return doneFuture;
    }

    @Override
    protected List<String> prepareCmdArgs(JacsService<File> jacsService) {
        FileCopyServiceDescriptor.FileCopyArgs fileCopyArgs = getArgs(jacsService);
        jacsService.setServiceCmd(getFullExecutableName(scriptName));
        ImmutableList.Builder<String> cmdLineBuilder = new ImmutableList.Builder<>();
        cmdLineBuilder.add(fileCopyArgs.sourceFilename);
        cmdLineBuilder.add(fileCopyArgs.targetFilename);
        if (fileCopyArgs.convertTo8Bits) {
            cmdLineBuilder.add("8");
        }
        return cmdLineBuilder.build();
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsService<File> jacsService) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    private FileCopyServiceDescriptor.FileCopyArgs getArgs(JacsService<File> jacsService) {
        FileCopyServiceDescriptor.FileCopyArgs fileCopyArgs = new FileCopyServiceDescriptor.FileCopyArgs();
        new JCommander(fileCopyArgs).parse(jacsService.getArgsArray());
        return fileCopyArgs;
    }

    @Override
    protected boolean checkForErrors(String l) {
        if (StringUtils.isNotBlank(l) && l.matches("(?i:.*(error|exception).*)")) {
            logger.error(l);
            return true;
        } else {
            return false;
        }
    }
}
