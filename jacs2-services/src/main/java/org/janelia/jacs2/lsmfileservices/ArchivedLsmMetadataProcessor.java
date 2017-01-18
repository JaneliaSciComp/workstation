package org.janelia.jacs2.lsmfileservices;

import com.beust.jcommander.JCommander;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.model.service.JacsServiceState;
import org.janelia.jacs2.model.service.ProcessingLocation;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.service.impl.AbstractServiceProcessor;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.JacsServiceDispatcher;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceComputationFactory;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

public class ArchivedLsmMetadataProcessor extends AbstractServiceProcessor<File> {

    @Inject
    ArchivedLsmMetadataProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                                 ServiceComputationFactory computationFactory,
                                 JacsServiceDataPersistence jacsServiceDataPersistence,
                                 @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                 Logger logger) {
        super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
    }

    @Override
    public File getResult(JacsServiceData jacsServiceData) {
        if (StringUtils.isNotBlank(jacsServiceData.getStringifiedResult())) {
            return new File(jacsServiceData.getStringifiedResult());
        } else {
            return null;
        }
    }

    @Override
    public void setResult(File result, JacsServiceData jacsServiceData) {
        if (result != null) {
            jacsServiceData.setStringifiedResult(result.toString());
        } else {
            jacsServiceData.setStringifiedResult(null);
        }
    }

    @Override
    protected ServiceComputation<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs args = getArgs(jacsServiceData);
        if (StringUtils.isBlank(args.archiveLSMFile)) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "Input LSM file name must be specified"));
        } else if (StringUtils.isBlank(args.outputLSMMetadata)) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "Output LSM metadata name must be specified"));
        }
        File lsmMetadataFile = getOutputFile(args);
        File workingLsmFile = getWorkingLsmFile(jacsServiceData, lsmMetadataFile);
        return submitChildService(jacsServiceData,
                new JacsServiceDataBuilder(jacsServiceData)
                        .setName("fileCopy")
                        .addArg("-src", getInputFile(args).getAbsolutePath())
                        .addArg("-dst", workingLsmFile.getAbsolutePath())
                        .setProcessingLocation(ProcessingLocation.CLUSTER) // fileCopy only works on the cluster for now
                        .build())
                .thenCompose(fc -> {
                    JacsServiceData extractLsmMetadataService =
                            new JacsServiceDataBuilder(jacsServiceData)
                                    .setName("lsmFileMetadata")
                                    .addArg("-inputLSM", workingLsmFile.getAbsolutePath())
                                    .addArg("-outputLSMMetadata", lsmMetadataFile.getAbsolutePath())
                                    .build();
                    return this.submitParentService(fc, extractLsmMetadataService);
                });
    }

    @Override
    protected ServiceComputation<File> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        JacsServiceData lsmServiceData = (JacsServiceData) preProcessingResult;
        // wait for the child services to complete and set the result to the result of the last computation (LSM metadata)
        return this.waitForCompletion(lsmServiceData)
                .thenApply(r -> {
                    File localLsmFile = new File(lsmServiceData.getStringifiedResult());
                    setResult(localLsmFile, jacsServiceData);
                    return localLsmFile;
                })
                .exceptionally(exc -> {
                    jacsServiceData.setState(JacsServiceState.CANCELED);
                    return null;
                });
    }

    protected ServiceComputation<File> postProcessData(File processingResult, JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(processingResult)
                .thenApply(f -> {
                    try {
                        ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs args = getArgs(jacsServiceData);
                        if (!args.keepIntermediateLSM) {
                            File lsmMetadataFile = getOutputFile(args);
                            File workingLsmFile = getWorkingLsmFile(jacsServiceData, lsmMetadataFile);
                            try {
                                logger.debug("Delete working LSM file {}", workingLsmFile);
                                Files.deleteIfExists(workingLsmFile.toPath());
                            } catch (IOException e) {
                                logger.error("Error deleting the working LSM file {}", workingLsmFile, e);
                            }
                        }
                        return f;
                    } catch (Exception e) {
                        throw new ComputationException(jacsServiceData, e);
                    }
                });
    }

    private File getWorkingLsmFile(JacsServiceData jacsServiceData, File lsmMetadataFile) {
        return new File(lsmMetadataFile.getParentFile(), jacsServiceData.getName() + "_" + jacsServiceData.getId() + "_working.lsm");
    }

    private ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs getArgs(JacsServiceData jacsServiceData) {
        ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs args = new ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
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

}
