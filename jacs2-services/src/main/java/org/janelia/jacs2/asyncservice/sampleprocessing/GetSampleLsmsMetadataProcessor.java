package org.janelia.jacs2.asyncservice.sampleprocessing;

import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.DefaultServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractAnyServiceResultHandler;
import org.janelia.jacs2.asyncservice.lsmfileservices.LsmFileMetadataProcessor;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.sample.SampleDataService;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
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
public class GetSampleLsmsMetadataProcessor extends AbstractBasicLifeCycleServiceProcessor<List<SampleImageMetadataFile>> {

    private final SampleDataService sampleDataService;
    private final GetSampleImageFilesProcessor getSampleImageFilesProcessor;
    private final LsmFileMetadataProcessor lsmFileMetadataProcessor;

    @Inject
    GetSampleLsmsMetadataProcessor(ServiceComputationFactory computationFactory,
                                   JacsServiceDataPersistence jacsServiceDataPersistence,
                                   @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                   SampleDataService sampleDataService,
                                   GetSampleImageFilesProcessor getSampleImageFilesProcessor,
                                   LsmFileMetadataProcessor lsmFileMetadataProcessor,
                                   Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.sampleDataService = sampleDataService;
        this.getSampleImageFilesProcessor = getSampleImageFilesProcessor;
        this.lsmFileMetadataProcessor = lsmFileMetadataProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new SampleServiceArgs());
    }

    @Override
    public ServiceResultHandler<List<SampleImageMetadataFile>> getResultHandler() {
        return new AbstractAnyServiceResultHandler<List<SampleImageMetadataFile>>() {
            @Override
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                return areAllDependenciesDone(jacsServiceData);
            }

            @Override
            public List<SampleImageMetadataFile> collectResult(JacsServiceData jacsServiceData) {
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
        };
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return new DefaultServiceErrorChecker(logger);
    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        SampleServiceArgs args = getArgs(jacsServiceData);

        JacsServiceData jacsServiceDataHierarchy = jacsServiceDataPersistence.findServiceHierarchy(jacsServiceData.getId());

        JacsServiceData getSampleLsmsServiceRef = getSampleImageFilesProcessor.createServiceData(new ServiceExecutionContext(jacsServiceData),
                new ServiceArg("-sampleId", args.sampleId.toString()),
                new ServiceArg("-objective", args.sampleObjective),
                new ServiceArg("-sampleDataDir", args.sampleDataDir)
        );
        JacsServiceData getSampleLsmsService = submitDependencyIfNotPresent(jacsServiceDataHierarchy, getSampleLsmsServiceRef);

        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsServiceData.getOwner(), args.sampleId, args.sampleObjective);

        return anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs()
                        .stream()
                        .flatMap(lsmp -> lsmp.getLsmFiles().stream())
                        .map(lsmf -> {
                            File lsmImageFile = SampleServicesUtils.getImageFile(Paths.get(args.sampleDataDir), lsmf);
                            File lsmMetadataFile = SampleServicesUtils.getImageMetadataFile(args.sampleDataDir, lsmImageFile);

                            JacsServiceData lsmMetadataService = lsmFileMetadataProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                                            .waitFor(getSampleLsmsService)
                                            .build(),
                                    new ServiceArg("-inputLSM", lsmImageFile.getAbsolutePath()),
                                    new ServiceArg("-outputLSMMetadata", lsmMetadataFile.getAbsolutePath()));

                            return submitDependencyIfNotPresent(jacsServiceDataHierarchy, lsmMetadataService);
                        }))
                .collect(Collectors.toList());
    }

    @Override
    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(jacsServiceData);
    }

    private SampleServiceArgs getArgs(JacsServiceData jacsServiceData) {
        return SampleServiceArgs.parse(jacsServiceData.getArgsArray(), new SampleServiceArgs());
    }

}
