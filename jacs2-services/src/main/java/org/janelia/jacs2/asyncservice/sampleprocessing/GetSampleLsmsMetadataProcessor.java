package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.lsmfileservices.LsmFileMetadataProcessor;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.sample.SampleDataService;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Named("getSampleLsmMetadata")
public class GetSampleLsmsMetadataProcessor extends AbstractServiceProcessor<List<SampleImageMetadataFile>> {

    private final SampleDataService sampleDataService;
    private final GetSampleImageFilesProcessor getSampleImageFilesProcessor;
    private final LsmFileMetadataProcessor lsmFileMetadataProcessor;

    @Inject
    GetSampleLsmsMetadataProcessor(JacsServiceEngine jacsServiceEngine,
                                   ServiceComputationFactory computationFactory,
                                   JacsServiceDataPersistence jacsServiceDataPersistence,
                                   @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                   SampleDataService sampleDataService,
                                   Logger logger,
                                   GetSampleImageFilesProcessor getSampleImageFilesProcessor,
                                   LsmFileMetadataProcessor lsmFileMetadataProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.sampleDataService = sampleDataService;
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
    protected ServiceComputation<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        return createComputation(jacsServiceData);
    }

    @Override
    protected ServiceComputation<JacsServiceData> processData(JacsServiceData jacsServiceData) {
        return createComputation(jacsServiceData);
    }

    @Override
    protected List<JacsServiceData> submitAllDependencies(JacsServiceData jacsServiceData) {
        SampleServiceArgs args = getArgs(jacsServiceData);
        JacsServiceData getSampleLsms = getSampleImageFilesProcessor.create(new ServiceExecutionContext(jacsServiceData),
                new ServiceArg("-sampleId", args.sampleId.toString()),
                new ServiceArg("-objective", args.sampleObjective),
                new ServiceArg("-sampleDataDir", args.sampleDataDir));

        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsServiceData.getOwner(), args.sampleId, args.sampleObjective);

        List<JacsServiceData> lsmMetadataServices = anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs()
                        .stream()
                        .flatMap(lsmp -> lsmp.getLsmFiles().stream())
                        .map(lsmf -> {
                            File lsmImageFile = SampleServicesUtils.getImageFile(Paths.get(args.sampleDataDir), lsmf);
                            File lsmMetadataFile = SampleServicesUtils.getImageMetadataFile(args.sampleDataDir, lsmImageFile);

                            return lsmFileMetadataProcessor.create(new ServiceExecutionContext.Builder(jacsServiceData)
                                            .waitFor(getSampleLsms)
                                            .build(),
                                    new ServiceArg("-inputLSM", lsmImageFile.getAbsolutePath()),
                                    new ServiceArg("-outputLSMMetadata", lsmMetadataFile.getAbsolutePath()));
                        }))
                .collect(Collectors.toList());
        return new ImmutableList.Builder<JacsServiceData>()
                    .add(getSampleLsms)
                    .addAll(lsmMetadataServices)
                    .build();
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        return true;
    }

    @Override
    protected List<SampleImageMetadataFile> retrieveResult(JacsServiceData jacsServiceData) {
        SampleServiceArgs args = getArgs(jacsServiceData);
        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsServiceData.getOwner(), args.sampleId, args.sampleObjective);
        Path destinationDirectory = Paths.get(args.sampleDataDir);
        // invoke child file copy services for all LSM files
        return anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs()
                        .stream()
                        .flatMap(lsmp -> lsmp.getLsmFiles().stream())
                        .map(lsmf -> {
                            SampleImageFile sif = new SampleImageFile();
                            sif.setId(lsmf.getId());
                            sif.setArchiveFilePath(lsmf.getFilepath());
                            sif.setWorkingFilePath(SampleServicesUtils.getImageFile(destinationDirectory, lsmf).getAbsolutePath());
                            sif.setArea(ar.getName());
                            sif.setChanSpec(lsmf.getChanSpec());
                            sif.setColorSpec(lsmf.getChannelColors());
                            sif.setObjective(ar.getObjective());

                            SampleImageMetadataFile lsmMetadata = new SampleImageMetadataFile();
                            File lsmImageFile = SampleServicesUtils.getImageFile(Paths.get(args.sampleDataDir), lsmf);
                            File lsmMetadataFile = SampleServicesUtils.getImageMetadataFile(args.sampleDataDir, lsmImageFile);
                            lsmMetadata.setSampleImageFile(sif);
                            lsmMetadata.setMetadataFilePath(lsmMetadataFile.getAbsolutePath());
                            return lsmMetadata;
                        }))
                .collect(Collectors.toList());
    }

    private SampleServiceArgs getArgs(JacsServiceData jacsServiceData) {
        return SampleServiceArgs.parse(jacsServiceData.getArgsArray(), new SampleServiceArgs());
    }

}
