package org.janelia.jacs2.asyncservice.lsmfileservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

public class ArchivedLsmMetadataProcessor extends AbstractServiceProcessor<File> {

    @Inject
    ArchivedLsmMetadataProcessor(JacsServiceEngine jacsServiceEngine,
                                 ServiceComputationFactory computationFactory,
                                 JacsServiceDataPersistence jacsServiceDataPersistence,
                                 @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                 Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
    }

    @Override
    public File getResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToFile(jacsServiceData.getStringifiedResult());
    }

    @Override
    public void setResult(File result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.fileToString(result));
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
        List<JacsServiceData> childServices = jacsServiceEngine.submitMultipleServices(
                ImmutableList.of(
                        new JacsServiceDataBuilder(jacsServiceData)
                                .setName("fileCopy")
                                .addArg("-src", getInputFile(args).getAbsolutePath())
                                .addArg("-dst", workingLsmFile.getAbsolutePath())
                                .setProcessingLocation(ProcessingLocation.CLUSTER) // fileCopy only works on the cluster for now
                                .build(),
                        new JacsServiceDataBuilder(jacsServiceData)
                                .setName("lsmFileMetadata")
                                .addArg("-inputLSM", workingLsmFile.getAbsolutePath())
                                .addArg("-outputLSMMetadata", lsmMetadataFile.getAbsolutePath())
                                .build()
                        )
        );
        return createServiceComputation(Iterables.getLast(childServices));
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

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs args = getArgs(jacsServiceData);
        File lsmMetadataFile = getOutputFile(args);
        return lsmMetadataFile.exists();
    }

    @Override
    protected File retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        ArchivedLsmMetadataServiceDescriptor.ArchivedLsmMetadataArgs args = getArgs(jacsServiceData);
        File lsmMetadataFile = getOutputFile(args);
        return lsmMetadataFile;
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
