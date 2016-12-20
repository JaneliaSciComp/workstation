package org.janelia.jacs2.fileservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.impl.AbstractExternalProcessComputation;
import org.janelia.jacs2.service.impl.JacsService;

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
public class FileCopyComputation extends AbstractExternalProcessComputation<Void> {

    private static final String DY_LIBRARY_PATH_VARNAME = "LD_LIBRARY_PATH";

    @PropertyValue(name = "VAA3D.LibraryPath")
    @Inject
    private String libraryPath;
    @PropertyValue(name = "Convert.ScriptPath")
    @Inject
    private String scriptName;


    @Override
    public CompletionStage<JacsService<Void>> preProcessData(JacsService<Void> jacsService) {
        CompletableFuture<JacsService<Void>> preProcess = new CompletableFuture<>();
        FileCopyServiceDescriptor.FileCopyArgs fileCopyArgs = getArgs(jacsService.getJacsServiceData());
        if (StringUtils.isBlank(fileCopyArgs.sourceFilename)) {
            preProcess.completeExceptionally(new IllegalArgumentException("Source file name must be specified"));
        } else if (StringUtils.isBlank(fileCopyArgs.targetFilename)) {
            preProcess.completeExceptionally(new IllegalArgumentException("Target file name must be specified"));
        } else {
            preProcess.complete(jacsService);
        }
        return preProcess;
    }

    @Override
    public CompletionStage<JacsService<Void>> isReadyToProcess(JacsService<Void> jacsService) {
        // this service has no child services
        return CompletableFuture.completedFuture(jacsService);
    }

    @Override
    public CompletionStage<JacsService<Void>> isDone(JacsService<Void> jacsService) {
        CompletableFuture<JacsService<Void>> doneFuture = new CompletableFuture<>();
        FileCopyServiceDescriptor.FileCopyArgs fileCopyArgs = getArgs(jacsService.getJacsServiceData());
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
    protected List<String> prepareCmdArgs(JacsServiceData jacsServiceData) {
        FileCopyServiceDescriptor.FileCopyArgs fileCopyArgs = getArgs(jacsServiceData);
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
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    private FileCopyServiceDescriptor.FileCopyArgs getArgs(JacsServiceData jacsServiceData) {
        FileCopyServiceDescriptor.FileCopyArgs fileCopyArgs = new FileCopyServiceDescriptor.FileCopyArgs();
        new JCommander(fileCopyArgs).parse(jacsServiceData.getArgsAsArray());
        return fileCopyArgs;
    }

}
