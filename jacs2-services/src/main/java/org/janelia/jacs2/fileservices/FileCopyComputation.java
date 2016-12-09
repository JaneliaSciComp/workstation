package org.janelia.jacs2.fileservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.impl.AbstractExternalProcessComputation;
import org.janelia.jacs2.service.impl.ExternalProcessRunner;
import org.janelia.jacs2.service.impl.ServiceComputation;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Named("fileCopyService")
public class FileCopyComputation extends AbstractExternalProcessComputation {

    private static final String DY_LIBRARY_PATH_VARNAME = "LD_LIBRARY_PATH";

    @Named("localProcessRunner") @Inject
    private ExternalProcessRunner processRunner;
    @PropertyValue(name = "VAA3D.LibraryPath")
    @Inject
    private String libraryPath;
    @PropertyValue(name = "Convert.ScriptPath")
    @Inject
    private String scriptName;


    static class FileCopyArgs {
        @Parameter(names = "-src", description = "Source file name", required = true)
        String sourceFilename;
        @Parameter(names = "-dst", description = "Destination file name or location", required = true)
        String targetFilename;
        @Parameter(names = "-mv", arity = 0, description = "If used the file will be moved to the target", required = false)
        boolean deleteSourceFile = false;
        @Parameter(names = "-convert8", arity = 0, description = "If set it converts the image to 8bit", required = false)
        boolean convertTo8Bits = false;
    }

    @Override
    protected ExternalProcessRunner getProcessRunner() {
        return processRunner;
    }

    @Override
    public CompletionStage<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        CompletableFuture<JacsServiceData> preProcess = new CompletableFuture<>();
        FileCopyArgs fileCopyArgs = getArgs(jacsServiceData);
        if (StringUtils.isBlank(fileCopyArgs.sourceFilename)) {
            preProcess.completeExceptionally(new IllegalArgumentException("Source file name must be specified"));
        } else if (StringUtils.isBlank(fileCopyArgs.targetFilename)) {
            preProcess.completeExceptionally(new IllegalArgumentException("Target file name must be specified"));
        } else {
            preProcess.complete(jacsServiceData);
        }
        return preProcess;
    }

    @Override
    public CompletionStage<JacsServiceData> isReady(JacsServiceData jacsServiceData) {
        // this service has no child services
        return CompletableFuture.completedFuture(jacsServiceData);
    }

    @Override
    public ServiceComputation submitChildServiceAsync(JacsServiceData childServiceData, JacsServiceData parentService) {
        throw new UnsupportedOperationException("FileCopyService does not support child services");
    }

    @Override
    public CompletionStage<JacsServiceData> isDone(JacsServiceData jacsServiceData) {
        CompletableFuture<JacsServiceData> doneFuture = new CompletableFuture<>();
        FileCopyArgs fileCopyArgs = getArgs(jacsServiceData);
        if (fileCopyArgs.deleteSourceFile) {
            try {
                File sourceFile = new File(fileCopyArgs.sourceFilename);
                Files.deleteIfExists(sourceFile.toPath());
            } catch (IOException e) {
                doneFuture.completeExceptionally(e);
            }
        } else {
            doneFuture.complete(jacsServiceData);
        }
        return doneFuture;
    }

    @Override
    protected List<String> prepareCmdArgs(JacsServiceData jacsServiceData) {
        FileCopyArgs fileCopyArgs = getArgs(jacsServiceData);
        jacsServiceData.setServiceCmd(getFullExecutableName(scriptName));
        ImmutableList.Builder<String> cmdLineBuilder = new ImmutableList.Builder<>();
        cmdLineBuilder.add(fileCopyArgs.sourceFilename);
        cmdLineBuilder.add(fileCopyArgs.targetFilename);
        if (fileCopyArgs.convertTo8Bits) {
            cmdLineBuilder.add("8");
        }
        return cmdLineBuilder.build();
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData si) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedLibraryPath());
    }

    private FileCopyArgs getArgs(JacsServiceData jacsServiceData) {
        FileCopyArgs fileCopyArgs = new FileCopyArgs();
        new JCommander(fileCopyArgs).parse(jacsServiceData.getArgsAsArray());
        return fileCopyArgs;
    }

    private String getUpdatedLibraryPath() {
        Optional<String> currentLibraryPath = getEnvVar(DY_LIBRARY_PATH_VARNAME);
        if (currentLibraryPath.isPresent()) {
            return libraryPath + ":" + currentLibraryPath.get();
        } else {
            return libraryPath;
        }
    }

}
