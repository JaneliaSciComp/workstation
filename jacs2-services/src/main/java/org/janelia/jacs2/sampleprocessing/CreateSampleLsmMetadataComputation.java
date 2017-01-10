package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.model.service.JacsServiceState;
import org.janelia.jacs2.service.dataservice.sample.SampleDataService;
import org.janelia.jacs2.service.impl.AbstractServiceComputation;
import org.janelia.jacs2.service.impl.JacsService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Named("sampleLsmMetadataService")
public class CreateSampleLsmMetadataComputation extends AbstractServiceComputation<List<File>> {

    private final SampleDataService sampleDataService;
    private final Logger logger;

    @Inject
    CreateSampleLsmMetadataComputation(SampleDataService sampleDataService, Logger logger) {
        this.sampleDataService = sampleDataService;
        this.logger = logger;
    }

    @Override
    public CompletionStage<JacsService<List<File>>> preProcessData(JacsService<List<File>> jacsService) {
        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs args = getArgs(jacsService);
        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsService.getOwner(), args.sampleId, args.sampleObjective);
        if (anatomicalAreas.isEmpty()) {
            CompletableFuture<JacsService<List<File>>> preProcessExc = new CompletableFuture<>();
            preProcessExc.completeExceptionally(new IllegalArgumentException("No anatomical areas found for " +
                    args.sampleId +
                    (StringUtils.isBlank(args.sampleObjective) ? "" : args.sampleObjective)));
            return preProcessExc;
        }
        List<Number> result = anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
                .map(lf -> {
                    File lsmMetadataFileName = getOutputFileName(args.outputDir, lf.getFilepath());
                    JacsServiceDataBuilder lsmMetadataServiceDataBuilder =
                            new JacsServiceDataBuilder(jacsService.getJacsServiceData())
                                    .setName("archivedLsmMetadata")
                                    .addArg("-archivedLSM", lf.getFilepath())
                                    .addArg("-outputLSMMetadata", lsmMetadataFileName.getAbsolutePath());
                    JacsServiceData lsmMetadataServiceData = lsmMetadataServiceDataBuilder.build();
                    jacsService.submitChildServiceAsync(lsmMetadataServiceData);
                    return lsmMetadataServiceData.getId();
                }).collect(Collectors.toList());
        if (result.isEmpty()) {
            jacsService.setState(JacsServiceState.CANCELED);
            CompletableFuture<JacsService<List<File>>> preProcessExc = new CompletableFuture<>();
            preProcessExc.completeExceptionally(new IllegalArgumentException("No LSM image found for " +
                    args.sampleId +
                    (StringUtils.isBlank(args.sampleObjective) ? "" : args.sampleObjective)));
            return preProcessExc;
        }
        logger.info("Created child services {} to extract metadata from the LSMs for {}", result, jacsService.getJacsServiceData());
        return super.preProcessData(jacsService);
    }

    @Override
    public CompletionStage<JacsService<List<File>>> processData(JacsService<List<File>> jacsService) {
        // collect the metadata files and update the corresponding LSMs
        CompletableFuture<JacsService<List<File>>> processData = new CompletableFuture<>();
        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs args = getArgs(jacsService);
        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsService.getOwner(), args.sampleId, args.sampleObjective);
        List<File> metadataFiles = new ArrayList<>();
        anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
                .forEach(lf -> {
                    File lsmMetadataFile = getOutputFileName(args.outputDir, lf.getFilepath());
                    sampleDataService.updateLMSMetadata(lf, lsmMetadataFile.getAbsolutePath());
                    metadataFiles.add(lsmMetadataFile);
                });
        if (metadataFiles.isEmpty()) {
            processData.completeExceptionally(new IllegalStateException("Something must have gone wrong because not all sample LSMs had their metadata updated"));
        } else {
            jacsService.setResult(metadataFiles);
            processData.complete(jacsService);
        }
        return processData;
    }

    private CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs getArgs(JacsService jacsService) {
        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs sampleLsmMetadataArgs = new CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs();
        new JCommander(sampleLsmMetadataArgs).parse(jacsService.getArgsArray());
        return sampleLsmMetadataArgs;
    }

    private File getOutputFileName(String outputDir, String inputFileName) {
        File inputFile = new File(inputFileName);
        return new File(outputDir, inputFile.getName().replaceAll("\\s+", "_") + ".json");
    }
}
