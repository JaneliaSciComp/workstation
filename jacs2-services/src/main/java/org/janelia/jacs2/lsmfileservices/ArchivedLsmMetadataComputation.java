package org.janelia.jacs2.lsmfileservices;

import com.beust.jcommander.JCommander;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.service.impl.AbstractServiceComputation;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.JacsService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Named("archivedLsmMetadataService")
public class ArchivedLsmMetadataComputation extends AbstractServiceComputation<File> {

    @Inject
    private Logger logger;

    @Override
    public CompletionStage<JacsService<File>> preProcessData(JacsService<File> jacsService) {
        CompletableFuture<JacsService<File>> preProcess = new CompletableFuture<>();
        ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs args = getArgs(jacsService);
        if (StringUtils.isBlank(args.archiveLSMFile)) {
            preProcess.completeExceptionally(new ComputationException(jacsService, "Input LSM file name must be specified"));
        } else if (StringUtils.isBlank(args.outputLSMMetadata)) {
            preProcess.completeExceptionally(new ComputationException(jacsService, "Output LSM metadata name must be specified"));
        } else {
            File lsmMetadataFile = getOutputFile(args);
            File workingLsmFile = getWorkingLsmFile(jacsService, lsmMetadataFile);
            JacsServiceData extractLsmMetadataService =
                    new JacsServiceDataBuilder(jacsService.getJacsServiceData())
                            .setName("lsmFileMetadata")
                            .addArg("-inputLSM", workingLsmFile.getAbsolutePath())
                            .addArg("-outputLSMMetadata", lsmMetadataFile.getAbsolutePath())
                            .build();
            extractLsmMetadataService.addChildService(new JacsServiceDataBuilder(jacsService.getJacsServiceData())
                    .setName("fileCopy")
                    .addArg("-src", getInputFile(args).getAbsolutePath())
                    .addArg("-dst", workingLsmFile.getAbsolutePath())
                    .build());
            jacsService.submitChildServiceAsync(extractLsmMetadataService);
            preProcess.complete(jacsService);
        }
        return preProcess;
    }

    private File getWorkingLsmFile(JacsService<File> jacsService, File lsmMetadataFile) {
        return new File(lsmMetadataFile.getParentFile(), jacsService.getName() + "_" + jacsService.getId() + "_working.lsm");
    }

    private ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs getArgs(JacsService jacsService) {
        ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs args = new ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs();
        new JCommander(args).parse(jacsService.getArgsArray());
        return args;
    }

    private File getInputFile(ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs args) {
        return new File(args.archiveLSMFile);
    }

    private File getOutputFile(ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs args) {
        try {
            File outputFile = new File(args.outputLSMMetadata);
            Files.createDirectories(outputFile.getParentFile().toPath());
            return outputFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public CompletionStage<JacsService<File>> processData(JacsService<File> jacsService) {
        return CompletableFuture.completedFuture(jacsService);
    }

    @Override
    public void postProcessData(JacsService<File> jacsService, Throwable exc) {
        if (exc == null) {
            ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs args = getArgs(jacsService);
            File lsmMetadataFile = getOutputFile(args);
            File workingLsmFile = getWorkingLsmFile(jacsService, lsmMetadataFile);
            try {
                Files.deleteIfExists(workingLsmFile.toPath());
            } catch (IOException e) {
                logger.error("Error deleting the working LSM file {}", workingLsmFile, e);
            }
        }
    }

}
