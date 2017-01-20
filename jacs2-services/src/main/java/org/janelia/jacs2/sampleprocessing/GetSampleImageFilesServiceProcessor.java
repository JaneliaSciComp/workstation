package org.janelia.jacs2.sampleprocessing;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.model.service.ProcessingLocation;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.service.dataservice.sample.SampleDataService;
import org.janelia.jacs2.service.impl.AbstractServiceProcessor;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.JacsServiceDispatcher;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceComputationFactory;
import org.janelia.jacs2.service.impl.ServiceDataUtils;
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
                    (StringUtils.isBlank(args.sampleObjective) ? "" : args.sampleObjective)));
        }
        Path destinationDirectory = Paths.get(args.sampleDataDir);
        Map<String, SampleImageFile> indexedSampleImageFiles = new LinkedHashMap<>();
        List<ServiceComputation<?>> fcs = new ArrayList<>();
        // invoke child file copy services for all LSM files
        anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
                .map(lf -> {
                    SampleImageFile sif = new SampleImageFile();
                    sif.setId(lf.getId());
                    sif.setArchiveFilePath(lf.getFilepath());
                    sif.setWorkingFilePath(getTargetImageFile(destinationDirectory, lf).getAbsolutePath());
                    sif.setArea(lf.getAnatomicalArea());
                    sif.setChanSpec(lf.getChanSpec());
                    sif.setColorSpec(lf.getChannelColors());
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
                    ServiceComputation<?> fc = this.submitChildService(jacsServiceData, retrieveImageFileServiceData)
                            .thenCompose(sd -> this.waitForCompletion(sd));
                    fcs.add(fc);
                });
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenCombineAll(fcs, (sd, results) -> results.stream().map(r -> indexedSampleImageFiles.get(((File) r).getAbsolutePath())).collect(Collectors.toList()))
                .thenApply(results -> {
                    setResult(results, jacsServiceData);
                    return results;
                });
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
