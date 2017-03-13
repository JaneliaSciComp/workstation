package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.fileservices.FileCopyProcessor;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.dataservice.sample.SampleDataService;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Named("getSampleImageFiles")
public class GetSampleImageFilesProcessor extends AbstractServiceProcessor<List<SampleImageFile>> {

    private final SampleDataService sampleDataService;
    private final FileCopyProcessor fileCopyProcessor;

    @Inject
    GetSampleImageFilesProcessor(JacsServiceEngine jacsServiceEngine,
                                 ServiceComputationFactory computationFactory,
                                 JacsServiceDataPersistence jacsServiceDataPersistence,
                                 @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                 SampleDataService sampleDataService,
                                 Logger logger,
                                 FileCopyProcessor fileCopyProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.sampleDataService = sampleDataService;
        this.fileCopyProcessor = fileCopyProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new SampleServiceArgs());
    }

    @Override
    public List<SampleImageFile> getResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToAny(jacsServiceData.getStringifiedResult(), new TypeReference<List<SampleImageFile>>(){});
    }

    @Override
    public void setResult(List<SampleImageFile> result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.anyToString(result));
    }

    @Override
    protected ServiceComputation<JacsServiceData> prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            SampleServiceArgs args = getArgs(jacsServiceData);
            Path destinationDirectory = Paths.get(args.sampleDataDir);
            Files.createDirectories(destinationDirectory);
        } catch (Exception e) {
            createFailure(e);
        }
        return createComputation(jacsServiceData);
    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        SampleServiceArgs args = getArgs(jacsServiceData);
        Path destinationDirectory = Paths.get(args.sampleDataDir);

        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsServiceData.getOwner(), args.sampleId, args.sampleObjective);
        if (anatomicalAreas.isEmpty()) {
            throw new ComputationException(jacsServiceData, "No anatomical areas found for " + args.sampleId +
                            (StringUtils.isBlank(args.sampleObjective) ? "" : "-" + args.sampleObjective));
        }
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
                            return sif;
                        }))
                .map(sif -> fileCopyProcessor.submit(new ServiceExecutionContext.Builder(jacsServiceData).processingLocation(ProcessingLocation.CLUSTER).build(),
                        new ServiceArg("-src", sif.getArchiveFilePath()),
                        new ServiceArg("-dst", sif.getWorkingFilePath())))
                .collect(Collectors.toList());
    }

    protected ServiceComputation<List<SampleImageFile>> processing(JacsServiceData jacsServiceData) {
        return createComputation(this.waitForResult(jacsServiceData));
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        return checkForDependenciesCompletion(jacsServiceData);
    }

    @Override
    protected List<SampleImageFile> retrieveResult(JacsServiceData jacsServiceData) {
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
                            return sif;
                        }))
                .collect(Collectors.toList());
    }

    private SampleServiceArgs getArgs(JacsServiceData jacsServiceData) {
        return SampleServiceArgs.parse(jacsServiceData.getArgsArray(), new SampleServiceArgs());
    }

}
