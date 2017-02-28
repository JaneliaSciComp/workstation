package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.sampleprocessing.zeiss.LSMDetectionChannel;
import org.janelia.jacs2.asyncservice.sampleprocessing.zeiss.LSMMetadata;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.dataservice.sample.SampleDataService;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named("updateLSMMetadata")
public class UpdateLSMsMetadataProcessor extends AbstractServiceProcessor<Void> {

    private final SampleDataService sampleDataService;
    private final GetSampleLsmsMetadataProcessor getSampleLsmsMetadataProcessor;

    @Inject
    UpdateLSMsMetadataProcessor(JacsServiceEngine jacsServiceEngine,
                                ServiceComputationFactory computationFactory,
                                JacsServiceDataPersistence jacsServiceDataPersistence,
                                @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                SampleDataService sampleDataService,
                                Logger logger,
                                GetSampleLsmsMetadataProcessor getSampleLsmsMetadataProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.sampleDataService = sampleDataService;
        this.getSampleLsmsMetadataProcessor = getSampleLsmsMetadataProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new SampleServiceArgs());
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
        SampleServiceArgs args = getArgs(jacsServiceData);
        return getSampleLsmsMetadataProcessor.invokeAsync(new ServiceExecutionContext(jacsServiceData),
                new ServiceArg("-sampleId", args.sampleId.toString()),
                new ServiceArg("-objective", args.sampleObjective),
                new ServiceArg("-sampleDataDir", args.sampleDataDir))
                .suspend(sd -> this.checkForCompletion(sd), sd -> sd)
                .thenApply(sd -> getSampleLsmsMetadataProcessor.getResult(sd));
    }

    @Override
    protected ServiceComputation<Void> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        List<SampleImageMetadataFile> sampleLSMsMetadata = (List<SampleImageMetadataFile>) preProcessingResult;
        if (CollectionUtils.isEmpty(sampleLSMsMetadata)) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "No sample image file was found"));
        }
        jacsServiceData.setState(JacsServiceState.RUNNING);
        jacsServiceDataPersistence.update(jacsServiceData);
        Map<String, SampleImageMetadataFile> indexedSampleLSMsMetadata = Maps.uniqueIndex(
                sampleLSMsMetadata,
                lsmf -> lsmf.getSampleImageFile().getId().toString());
        List<LSMImage> lsmImages = sampleDataService.getLSMsByIds(jacsServiceData.getOwner(), sampleLSMsMetadata.stream().map(lsmMetadata -> lsmMetadata.getSampleImageFile().getId()).collect(Collectors.toList()));
        lsmImages.forEach(lsm -> {
            String lsmId = lsm.getId().toString();
            SampleImageMetadataFile lsmMetadataFile = indexedSampleLSMsMetadata.get(lsmId);
            if (lsmMetadataFile == null) {
                throw new ComputationException(jacsServiceData, "Internal error: No LSM metadata found for " + lsmId + " even though the id was used to retrieve the LSM image");
            }
            sampleDataService.updateLSMMetadataFile(lsm, lsmMetadataFile.getMetadataFilePath());
            try {
                // read the metadata from the metadata file
                LSMMetadata lsmMetadata = LSMMetadata.fromFile(new File(lsmMetadataFile.getMetadataFilePath()));
                List<String> colors = new ArrayList<>();
                List<String> dyeNames = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(lsmMetadata.getChannels())) {
                    lsmMetadata.getChannels().forEach(channel -> {
                        colors.add(channel.getColor());
                        LSMDetectionChannel detection = lsmMetadata.getDetectionChannel(channel);
                        if (detection!=null) {
                            dyeNames.add(detection.getDyeName());
                        }
                        else {
                            dyeNames.add("Unknown");
                        }
                    });
                }
                boolean lsmUpdated = true;
                if (CollectionUtils.isNotEmpty(colors)) {
                    lsm.setChannelColors(Joiner.on(',').join(colors));
                    lsmUpdated = true;
                }
                if (CollectionUtils.isNotEmpty(dyeNames)) {
                    lsm.setChannelDyeNames(Joiner.on(',').join(dyeNames));
                    lsmUpdated = true;
                }
                if (lsmUpdated) {
                    sampleDataService.updateLSM(lsm);
                }
            } catch (Exception e) {
                throw new ComputationException(jacsServiceData, e);
            }
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
