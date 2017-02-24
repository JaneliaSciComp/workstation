package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.lsmfileservices.LsmFileMetadataProcessor;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Named("getSampleLsmMetadata")
public class GetSampleLsmsMetadataProcessor extends AbstractServiceProcessor<List<SampleImageMetadataFile>> {

    private final GetSampleImageFilesProcessor getSampleImageFilesProcessor;
    private final LsmFileMetadataProcessor lsmFileMetadataProcessor;

    @Inject
    GetSampleLsmsMetadataProcessor(JacsServiceEngine jacsServiceEngine,
                                   ServiceComputationFactory computationFactory,
                                   JacsServiceDataPersistence jacsServiceDataPersistence,
                                   @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                   Logger logger,
                                   GetSampleImageFilesProcessor getSampleImageFilesProcessor,
                                   LsmFileMetadataProcessor lsmFileMetadataProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.getSampleImageFilesProcessor = getSampleImageFilesProcessor;
        this.lsmFileMetadataProcessor = lsmFileMetadataProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new SampleServiceArgs());
    }

    @Override
    public List<SampleImageMetadataFile> getResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToAny(jacsServiceData.getStringifiedResult(), new TypeReference<List<SampleImageMetadataFile>>(){});
    }

    @Override
    public void setResult(List<SampleImageMetadataFile> result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.anyToString(result));
    }

    @Override
    protected ServiceComputation<List<SampleImageFile>> preProcessData(JacsServiceData jacsServiceData) {
        SampleServiceArgs args = getArgs(jacsServiceData);
        return getSampleImageFilesProcessor.invokeAsync(new ServiceExecutionContext(jacsServiceData),
                "-sampleId", args.sampleId.toString(),
                "-objective", args.sampleObjective,
                "-sampleDataDir", args.sampleDataDir)
                .thenCompose(sd -> this.waitForCompletion(sd))
                .thenApply(sd -> getSampleImageFilesProcessor.getResult(sd));
    }

    @Override
    protected ServiceComputation<List<SampleImageMetadataFile>> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        SampleServiceArgs args = getArgs(jacsServiceData);
        List<SampleImageFile> sampleLSMs = (List<SampleImageFile>) preProcessingResult;
        if (CollectionUtils.isEmpty(sampleLSMs)) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "No sample image file was found"));
        }
        List<ServiceComputation<?>> lsmMetadataComputations = submitAllLSMMetadataServices(sampleLSMs, args.sampleDataDir, jacsServiceData);
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenCombineAll(lsmMetadataComputations, (sd, sampleLSMMetadataResults) -> sampleLSMMetadataResults.stream().map(r -> (SampleImageMetadataFile) r).collect(Collectors.toList()))
                .thenApply(results -> this.applyResult(results, jacsServiceData));
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<SampleImageMetadataFile> retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        throw new UnsupportedOperationException();
    }

    private List<ServiceComputation<?>> submitAllLSMMetadataServices(List<SampleImageFile> lsmFiles, String outputDir, JacsServiceData jacsServiceData) {
        List<ServiceComputation<?>> lsmMetadataComputations = new ArrayList<>();
        lsmFiles.forEach(lsmf -> {
            File lsmMetadataFile = getOutputFileName(outputDir, new File(lsmf.getWorkingFilePath()));
            lsmMetadataComputations.add(
                    lsmFileMetadataProcessor.invokeAsync(new ServiceExecutionContext(jacsServiceData),
                            "-inputLSM", lsmf.getWorkingFilePath(),
                            "-outputLSMMetadata", lsmMetadataFile.getAbsolutePath())
                            .thenCompose(sd -> this.waitForCompletion(sd))
                            .thenApply(sd -> lsmFileMetadataProcessor.getResult(sd))
                            .thenApply(f -> {
                                SampleImageMetadataFile lsmMetadata = new SampleImageMetadataFile();
                                lsmMetadata.setSampleImageFile(lsmf);
                                lsmMetadata.setMetadataFilePath(f.getAbsolutePath());
                                return lsmMetadata;
                            }));
        });
        return lsmMetadataComputations;
    }

    private SampleServiceArgs getArgs(JacsServiceData jacsServiceData) {
        return SampleServiceArgs.parse(jacsServiceData.getArgsArray(), new SampleServiceArgs());
    }

    private File getOutputFileName(String outputDir, File inputFile) {
        return new File(outputDir, inputFile.getName().replaceAll("\\s+", "_") + ".json");
    }
}
