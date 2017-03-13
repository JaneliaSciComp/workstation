package org.janelia.jacs2.asyncservice.lsmfileservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.fileservices.FileCopyProcessor;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

@Named("archivedLsmMetadata")
public class ArchivedLsmMetadataProcessor extends AbstractServiceProcessor<File> {

    static class ArchivedLsmMetadataArgs extends ServiceArgs {
        @Parameter(names = "-archivedLSM", description = "Archived LSM file name", required = true)
        String archiveLSMFile;
        @Parameter(names = "-outputLSMMetadata", description = "Destination directory", required = true)
        String outputLSMMetadata;
        @Parameter(names = "-keepIntermediateLSM", arity = 0, description = "If used the temporary LSM file created from the archive will not be deleted", required = false)
        boolean keepIntermediateLSM = false;
    }

    private final FileCopyProcessor fileCopyProcessor;
    private final LsmFileMetadataProcessor lsmFileMetadataProcessor;

    @Inject
    ArchivedLsmMetadataProcessor(JacsServiceEngine jacsServiceEngine,
                                 ServiceComputationFactory computationFactory,
                                 JacsServiceDataPersistence jacsServiceDataPersistence,
                                 @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                 Logger logger,
                                 FileCopyProcessor fileCopyProcessor,
                                 LsmFileMetadataProcessor lsmFileMetadataProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.fileCopyProcessor = fileCopyProcessor;
        this.lsmFileMetadataProcessor = lsmFileMetadataProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new ArchivedLsmMetadataArgs());
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
    protected ServiceComputation<JacsServiceData> prepareProcessing(JacsServiceData jacsServiceData) {
        ArchivedLsmMetadataArgs args = getArgs(jacsServiceData);
        if (StringUtils.isBlank(args.archiveLSMFile)) {
            return createFailure(new ComputationException(jacsServiceData, "Input LSM file name must be specified"));
        } else if (StringUtils.isBlank(args.outputLSMMetadata)) {
            return createFailure(new ComputationException(jacsServiceData, "Output LSM metadata name must be specified"));
        } else {
            return createComputation(jacsServiceData);
        }
    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        ArchivedLsmMetadataArgs args = getArgs(jacsServiceData);
        File lsmMetadataFile = getOutputFile(args);
        File workingLsmFile = getWorkingLsmFile(jacsServiceData, lsmMetadataFile);
        JacsServiceData fileCopyInvocation = fileCopyProcessor.submit(new ServiceExecutionContext.Builder(jacsServiceData).processingLocation(ProcessingLocation.CLUSTER).build(),
                new ServiceArg("-src", getInputFile(args).getAbsolutePath()),
                new ServiceArg("-dst", workingLsmFile.getAbsolutePath()));
        JacsServiceData lsmMetadataInvocation = lsmFileMetadataProcessor.submit(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(fileCopyInvocation)
                        .build(),
                new ServiceArg("-inputLSM", workingLsmFile.getAbsolutePath()),
                new ServiceArg("-outputLSMMetadata", lsmMetadataFile.getAbsolutePath()));
        return ImmutableList.of(lsmMetadataInvocation);
    }

    @Override
    protected ServiceComputation<File> processing(JacsServiceData jacsServiceData) {
        return createComputation(this.waitForResult(jacsServiceData));
    }

    protected ServiceComputation<File> postProcessing(JacsServiceData jacsServiceData, File result) {
        return createComputation(result)
                .thenApply(r -> {
                    try {
                        ArchivedLsmMetadataArgs args = getArgs(jacsServiceData);
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
                        return r;
                    } catch (Exception e) {
                        throw new ComputationException(jacsServiceData, e);
                    }
                });
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        ArchivedLsmMetadataArgs args = getArgs(jacsServiceData);
        File lsmMetadataFile = getOutputFile(args);
        return lsmMetadataFile.exists();
    }

    @Override
    protected File retrieveResult(JacsServiceData jacsServiceData) {
        ArchivedLsmMetadataArgs args = getArgs(jacsServiceData);
        return getOutputFile(args);
    }

    private File getWorkingLsmFile(JacsServiceData jacsServiceData, File lsmMetadataFile) {
        return new File(lsmMetadataFile.getParentFile(), jacsServiceData.getName() + "_" + jacsServiceData.getId() + "_working.lsm");
    }

    private ArchivedLsmMetadataArgs getArgs(JacsServiceData jacsServiceData) {
        ArchivedLsmMetadataArgs args = new ArchivedLsmMetadataArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private File getInputFile(ArchivedLsmMetadataArgs args) {
        return new File(args.archiveLSMFile);
    }

    private File getOutputFile(ArchivedLsmMetadataArgs args) {
        try {
            File outputFile = new File(args.outputLSMMetadata);
            Files.createDirectories(outputFile.getParentFile().toPath());
            return outputFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
