package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.dataservice.sample.SampleDataService;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.JacsServiceDispatcher;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class UpdateLSMsMetadataProcessor extends AbstractServiceProcessor<Void> {

    private final SampleDataService sampleDataService;

    @Inject
    UpdateLSMsMetadataProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                                ServiceComputationFactory computationFactory,
                                JacsServiceDataPersistence jacsServiceDataPersistence,
                                @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                SampleDataService sampleDataService,
                                Logger logger) {
        super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.sampleDataService = sampleDataService;
    }

    @Override
    public Void getResult(JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public void setResult(Void result, JacsServiceData jacsServiceData) {
    }

    @Override
    protected ServiceComputation<List<SampleImageMetadataFile>> preProcessData(JacsServiceData jacsServiceData) {
        JacsServiceData sampleLSMsMetadataServiceData = SampleServicesUtils.createChildSampleServiceData("getSampleLsmMetadata", getArgs(jacsServiceData), jacsServiceData);
        return submitServiceDependency(jacsServiceData, sampleLSMsMetadataServiceData)
                .thenCompose(sd -> this.waitForCompletion(sd))
                .thenApply(r -> ServiceDataUtils.stringToAny(sampleLSMsMetadataServiceData.getStringifiedResult(), new TypeReference<List<SampleImageMetadataFile>>() {}));
    }

    @Override
    protected ServiceComputation<Void> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        List<SampleImageMetadataFile> sampleLSMsMetadata = (List<SampleImageMetadataFile>) preProcessingResult;
        if (CollectionUtils.isEmpty(sampleLSMsMetadata)) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "No sample image file was found"));
        }
        Map<Number, SampleImageMetadataFile> indexedSampleLSMsMetadata = Maps.uniqueIndex(
                sampleLSMsMetadata,
                lsmf -> lsmf.getSampleImageFile().getId());
        Map<Number, LSMImage> lsmImages = Maps.uniqueIndex(
                sampleDataService.getLSMsByIds(jacsServiceData.getOwner(), ImmutableList.copyOf(indexedSampleLSMsMetadata.keySet())),
                lsm -> lsm.getId());
        lsmImages.forEach((id, lsm) -> {
            SampleImageMetadataFile lsmMetadataFile = indexedSampleLSMsMetadata.get(id);
            if (lsmMetadataFile == null) {
                throw new ComputationException(jacsServiceData, "Internal error: No LSM metadata found for " + id + " even though the id was used to retrieve the LSM image");
            }
            sampleDataService.updateLMSMetadataFile(lsm, lsmMetadataFile.getMetadataFilePath());
            // read the metadata from the metadata file
            // TODO read the metadata
        });
        return computationFactory.newCompletedComputation(null);
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Void retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        throw new UnsupportedOperationException();
    }

    private SampleServiceArgs getArgs(JacsServiceData jacsServiceData) {
        return SampleServiceArgs.parse(jacsServiceData.getArgsArray(), new SampleServiceArgs());
    }

}
