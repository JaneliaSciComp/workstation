package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
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
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
    protected List<JacsServiceData> submitAllDependencies(JacsServiceData jacsServiceData) {
        SampleServiceArgs args = getArgs(jacsServiceData);
        JacsServiceData getSampleLsmMetadataService = getSampleLsmsMetadataProcessor.create(new ServiceExecutionContext(jacsServiceData),
                new ServiceArg("-sampleId", args.sampleId.toString()),
                new ServiceArg("-objective", args.sampleObjective),
                new ServiceArg("-sampleDataDir", args.sampleDataDir));

        return ImmutableList.of(getSampleLsmMetadataService);
    }

    @Override
    protected ServiceComputation<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        return createComputation(jacsServiceData);
    }

    @Override
    protected ServiceComputation<JacsServiceData> processData(JacsServiceData jacsServiceData) {
        SampleServiceArgs args = getArgs(jacsServiceData);
        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsServiceData.getOwner(), args.sampleId, args.sampleObjective);
        Path destinationDirectory = Paths.get(args.sampleDataDir);
        anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs()
                        .stream()
                        .flatMap(lsmp -> lsmp.getLsmFiles().stream()))
                .forEach(lsmf -> {
                    File lsmImageFile = SampleServicesUtils.getImageFile(destinationDirectory, lsmf);
                    File lsmMetadataFile = SampleServicesUtils.getImageMetadataFile(args.sampleDataDir, lsmImageFile);
                    sampleDataService.updateLSMMetadataFile(lsmf, lsmMetadataFile.getAbsolutePath());

                    // read the metadata from the metadata file
                    try {
                        LSMMetadata lsmMetadata = LSMMetadata.fromFile(lsmMetadataFile);
                        List<String> colors = new ArrayList<>();
                        List<String> dyeNames = new ArrayList<>();
                        if (CollectionUtils.isNotEmpty(lsmMetadata.getChannels())) {
                            lsmMetadata.getChannels().forEach(channel -> {
                                colors.add(channel.getColor());
                                LSMDetectionChannel detection = lsmMetadata.getDetectionChannel(channel);
                                if (detection != null) {
                                    dyeNames.add(detection.getDyeName());
                                } else {
                                    dyeNames.add("Unknown");
                                }
                            });
                        }
                        boolean lsmUpdated = false;
                        if (CollectionUtils.isNotEmpty(colors)) {
                            lsmf.setChannelColors(Joiner.on(',').join(colors));
                            lsmUpdated = true;
                        }
                        if (CollectionUtils.isNotEmpty(dyeNames)) {
                            lsmf.setChannelDyeNames(Joiner.on(',').join(dyeNames));
                            lsmUpdated = true;
                        }
                        if (lsmUpdated) {
                            sampleDataService.updateLSM(lsmf);
                        }
                    } catch (IOException e) {
                        throw new ComputationException(jacsServiceData, e);
                    }
                });
        return createComputation(jacsServiceData);
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        return true;
    }

    @Override
    protected Void retrieveResult(JacsServiceData jacsServiceData) {
        return null;
    }

    private SampleServiceArgs getArgs(JacsServiceData jacsServiceData) {
        return SampleServiceArgs.parse(jacsServiceData.getArgsArray(), new SampleServiceArgs());
    }

}
