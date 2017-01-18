package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import org.apache.commons.collections4.CollectionUtils;
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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GetSampleImageFilesServiceProcessor extends AbstractServiceProcessor<List<File>> {

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
    public List<File> getResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToFileList(jacsServiceData.getStringifiedResult());
    }

    @Override
    public void setResult(List<File> result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.fileListToString(result));
    }

    @Override
    protected ServiceComputation<List<File>> preProcessData(JacsServiceData jacsServiceData) {
        GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs args = getArgs(jacsServiceData);
        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsServiceData.getOwner(), args.sampleId, args.sampleObjective);
        if (anatomicalAreas.isEmpty()) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "No anatomical areas found for " +
                    args.sampleId +
                    (StringUtils.isBlank(args.sampleObjective) ? "" : args.sampleObjective)));
        }
        Path workingDirectory = getWorkingDirectory(jacsServiceData);
        // invoke child file copy services for all LSM files
        List<ServiceComputation<?>> fcs = anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
                .map(lf -> {
                    JacsServiceData retrieveImageFileServiceData =
                            new JacsServiceDataBuilder(jacsServiceData)
                                    .setName("fileCopy")
                                    .addArg("-src", lf.getFilepath())
                                    .addArg("-dst", getIntermediateImageFile(workingDirectory, lf).getAbsolutePath())
                                    .setProcessingLocation(ProcessingLocation.CLUSTER) // fileCopy only works on the cluster for now
                                    .build();
                    this.submitChildService(jacsServiceData, retrieveImageFileServiceData);
                    return this.waitForCompletion(retrieveImageFileServiceData);
                })
                .collect(Collectors.toList());
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenCombineAll(fcs, (sd, results) -> results.stream().map(r -> (File)r).collect(Collectors.toList()));
    }

    @Override
    protected ServiceComputation<List<File>> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        // collect the files and send them to the destination
        GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs args = getArgs(jacsServiceData);
        List<File> intermediateFiles = (List<File>) preProcessingResult;
        List<File> results = new ArrayList<>();
        intermediateFiles.forEach(imageFile -> results.add(copyFileToFolder(imageFile, Paths.get(args.destFolder))));
        setResult(results, jacsServiceData);
        return computationFactory.newCompletedComputation(results);
    }

    protected ServiceComputation<List<File>> postProcessData(List<File> processingResult, JacsServiceData jacsServiceData) {
        try {
            GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs args = getArgs(jacsServiceData);
            if (CollectionUtils.isNotEmpty(processingResult)) {
                List<AnatomicalArea> anatomicalAreas =
                        sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsServiceData.getOwner(), args.sampleId, args.sampleObjective);
                Path workingDirectory = getWorkingDirectory(jacsServiceData);
                anatomicalAreas.stream()
                        .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
                        .map(lf -> getIntermediateImageFile(workingDirectory, lf).toString())
                        .filter(workingFileName -> !processingResult.contains(workingFileName)) // only delete the file if it's not an actual result
                        .forEach(workingFileName -> {
                            try {
                                Files.deleteIfExists(Paths.get(workingFileName));
                            } catch (IOException e) {
                                logger.warn("Error deleting working file {}", workingFileName, e);
                            }
                        });
            }
            return computationFactory.newCompletedComputation(processingResult);
        } catch (Exception e) {
            return computationFactory.newFailedComputation(e);
        }
    }

    private File copyFileToFolder(File imageFile, Path destFolder) {
        String fileName = imageFile.getName();
        Path destFile = destFolder.resolve(fileName);
        try {
            Files.createDirectories(destFolder); // ensure the destination folder exists
            Files.copy(imageFile.toPath(), destFile);
            return destFile.toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs getArgs(JacsServiceData jacsServiceData) {
        GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs args = new GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private File getIntermediateImageFile(Path workingDir, Image image) {
        String fileName = new File(image.getFilepath()).getName();
        if (fileName.endsWith(".bz2")) {
            fileName = fileName.substring(0, fileName.length() - ".bz2".length());
        } else if (fileName.endsWith(".gz")) {
            fileName = fileName.substring(0, fileName.length() - ".gz".length());
        }
        return new File(workingDir.toFile(), fileName);
    }
}
