package org.janelia.jacs2.sampleprocessing;

import com.fasterxml.jackson.core.type.TypeReference;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.service.impl.AbstractServiceProcessor;
import org.janelia.jacs2.service.impl.JacsServiceDispatcher;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceComputationFactory;
import org.janelia.jacs2.service.impl.ServiceDataUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GetSampleLsmsMetadataProcessor extends AbstractServiceProcessor<List<File>> {

    @Inject
    GetSampleLsmsMetadataProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                                   ServiceComputationFactory computationFactory,
                                   JacsServiceDataPersistence jacsServiceDataPersistence,
                                   @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                   Logger logger) {
        super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
    }

    @Override
    public List<File> getResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToFileList(jacsServiceData.getStringifiedResult());
    }

    @Override
    public void setResult(List<File> result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.fileListToString(result));
    }

    @Override
    protected ServiceComputation<List<SampleImageFile>> preProcessData(JacsServiceData jacsServiceData) {
        JacsServiceData sampleLSMsServiceData = SampleServicesUtils.createChildSampleServiceData("getSampleImageFiles", getArgs(jacsServiceData), jacsServiceData);
        return submitChildService(jacsServiceData, sampleLSMsServiceData)
                .thenCompose(sd -> this.waitForCompletion(sd))
                .thenApply(r -> ServiceDataUtils.stringToAny(sampleLSMsServiceData.getStringifiedResult(), new TypeReference<List<SampleImageFile>>() {}));
    }


    @Override
    protected ServiceComputation<List<File>> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        SampleServiceArgs args = getArgs(jacsServiceData);
        List<SampleImageFile> sampleLSMs = (List<SampleImageFile>) preProcessingResult;

        List<ServiceComputation<?>> lsmMetadataComputations = submitAllLSMMetadataServices(sampleLSMs, args.sampleDataDir, jacsServiceData);
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenCombineAll(lsmMetadataComputations, (sd, sampleLSMMetadataResults) -> sampleLSMMetadataResults.stream().map(r -> (File)r).collect(Collectors.toList()));
    }

    private List<ServiceComputation<?>> submitAllLSMMetadataServices(List<SampleImageFile> lsmFiles, String outputDir, JacsServiceData jacsServiceData) {
        List<ServiceComputation<?>> lsmMetadataComputations = new ArrayList<>();
        lsmFiles.forEach(f -> {
            File lsmMetadataFile = getOutputFileName(outputDir, new File(f.getWorkingFilePath()));
            JacsServiceData extractLsmMetadataService =
                    new JacsServiceDataBuilder(jacsServiceData)
                            .setName("lsmFileMetadata")
                            .addArg("-inputLSM", f.getWorkingFilePath())
                            .addArg("-outputLSMMetadata", lsmMetadataFile.getAbsolutePath())
                            .build();
            lsmMetadataComputations.add(
                    this.submitChildService(jacsServiceData, extractLsmMetadataService)
                            .thenCompose(sd -> this.waitForCompletion(sd))
            );
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
