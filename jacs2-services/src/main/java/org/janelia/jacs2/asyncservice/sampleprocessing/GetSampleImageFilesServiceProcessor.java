package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;
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
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GetSampleImageFilesServiceProcessor extends AbstractServiceProcessor<List<SampleImageFile>> {

    private final SampleDataService sampleDataService;

    @Inject
    GetSampleImageFilesServiceProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                                        ServiceComputationFactory computationFactory,
                                        JacsServiceDataPersistence jacsServiceDataPersistence,
                                        @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                        SampleDataService sampleDataService,
                                        Logger logger) {
        super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.sampleDataService = sampleDataService;
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
    protected ServiceComputation<List<SampleImageFile>> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        SampleServiceArgs args = getArgs(jacsServiceData);
        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsServiceData.getOwner(), args.sampleId, args.sampleObjective);
        if (anatomicalAreas.isEmpty()) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "No anatomical areas found for " +
                    args.sampleId +
                    (StringUtils.isBlank(args.sampleObjective) ? "" : "-" + args.sampleObjective)));
        }
        Path destinationDirectory = Paths.get(args.sampleDataDir);
        Map<String, SampleImageFile> indexedSampleImageFiles = new LinkedHashMap<>();
        List<ServiceComputation<?>> fcs = new ArrayList<>();
        // invoke child file copy services for all LSM files
        anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
                .map(lsmf -> {
                    SampleImageFile sif = new SampleImageFile();
                    sif.setId(lsmf.getId());
                    sif.setArchiveFilePath(lsmf.getFilepath());
                    sif.setWorkingFilePath(getTargetImageFile(destinationDirectory, lsmf).getAbsolutePath());
                    sif.setArea(lsmf.getAnatomicalArea());
                    sif.setChanSpec(lsmf.getChanSpec());
                    sif.setColorSpec(lsmf.getChannelColors());
                    return sif;
                })
                .forEach(sif -> {
                    JacsServiceData retrieveImageFileServiceData =
                            new JacsServiceDataBuilder(jacsServiceData)
                                    .setName("fileCopy")
                                    .addArg("-src", sif.getArchiveFilePath())
                                    .addArg("-dst", sif.getWorkingFilePath())
                                    .setProcessingLocation(ProcessingLocation.CLUSTER) // fileCopy only works on the cluster for now
                                    .build();
                    indexedSampleImageFiles.put(sif.getWorkingFilePath(), sif);
                    ServiceComputation<?> fc = this.submitServiceDependency(jacsServiceData, retrieveImageFileServiceData)
                            .thenCompose(sd -> this.waitForCompletion(sd));
                    fcs.add(fc);
                });
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenCombineAll(fcs, (sd, results) -> results.stream().map(r -> indexedSampleImageFiles.get(((File) r).getAbsolutePath())).collect(Collectors.toList()))
                .thenApply(results -> this.applyResult(results, jacsServiceData));
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<SampleImageFile> retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        throw new UnsupportedOperationException();
    }

    private SampleServiceArgs getArgs(JacsServiceData jacsServiceData) {
        return SampleServiceArgs.parse(jacsServiceData.getArgsArray(), new SampleServiceArgs());
    }

    private File getTargetImageFile(Path destDir, Image image) {
        String fileName = new File(image.getFilepath()).getName();
        if (fileName.endsWith(".bz2")) {
            fileName = fileName.substring(0, fileName.length() - ".bz2".length());
        } else if (fileName.endsWith(".gz")) {
            fileName = fileName.substring(0, fileName.length() - ".gz".length());
        }
        return new File(destDir.toFile(), fileName);
    }
}
