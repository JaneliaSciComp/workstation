package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
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
import java.util.Optional;
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
        JacsServiceData serviceData = jacsService.getJacsServiceData();
        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs sampleLsmMetadataArgs = getArgs(serviceData);
        Optional<AnatomicalArea> anatomicalArea =
                sampleDataService.getAnatomicalAreaBySampleIdAndObjective(jacsService.getOwner(), sampleLsmMetadataArgs.sampleId, sampleLsmMetadataArgs.sampleObjective);
        if (!anatomicalArea.isPresent()) {
            CompletableFuture<JacsService<List<File>>> preProcessExc = new CompletableFuture<>();
            preProcessExc.completeExceptionally(new IllegalArgumentException("No anatomical area found for " + serviceData));
            return preProcessExc;
        }
        Optional<List<Number>> result = anatomicalArea.map(ar -> {
            List<LSMImage> lsmFiles = ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()).collect(Collectors.toList());
            return lsmFiles;
        }).map(lsmFiles -> {
            List<File> metadataFiles = new ArrayList<>();
            List<Number> childServiceIds = new ArrayList<>();
            lsmFiles.forEach(lf -> {
                File lsmMetadataFileName = getOutputFileName(sampleLsmMetadataArgs.outputDir, lf.getFilepath());
                JacsServiceDataBuilder lsmMetadataServiceDataBuilder =
                        new JacsServiceDataBuilder(serviceData)
                                .setName("archivedLsmMetadata")
                                .addArg("-archivedLSM", lf.getFilepath())
                                .addArg("-outputLSMMetadata", lsmMetadataFileName.getAbsolutePath());
                if (sampleLsmMetadataArgs.keepIntermediateLSMFiles) {
                    lsmMetadataServiceDataBuilder.addArg("-keepIntermediateLSM");
                }
                JacsServiceData lsmMetadataServiceData = lsmMetadataServiceDataBuilder.build();
                jacsService.submitChildServiceAsync(lsmMetadataServiceData);
                childServiceIds.add(lsmMetadataServiceData.getId());
                metadataFiles.add(lsmMetadataFileName);
            });
            return childServiceIds;
        });
        if (!result.isPresent() || result.get().isEmpty()) {
            jacsService.setState(JacsServiceState.CANCELED);
            CompletableFuture<JacsService<List<File>>> preProcessExc = new CompletableFuture<>();
            preProcessExc.completeExceptionally(new IllegalArgumentException("No LSM image found for " + serviceData));
            return preProcessExc;
        }
        logger.info("Created child services {} to extract metadata from the LSMs for {}", result.get(), serviceData);
        return super.preProcessData(jacsService);
    }

    @Override
    public CompletionStage<JacsService<List<File>>> processData(JacsService<List<File>> jacsService) {
        // collect the metadata files and update the corresponding LSMs
        JacsServiceData serviceData = jacsService.getJacsServiceData();
        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs sampleLsmMetadataArgs = getArgs(serviceData);
        Optional<AnatomicalArea> anatomicalArea =
                sampleDataService.getAnatomicalAreaBySampleIdAndObjective(jacsService.getOwner(), sampleLsmMetadataArgs.sampleId, sampleLsmMetadataArgs.sampleObjective);
        CompletableFuture<JacsService<List<File>>> processData = new CompletableFuture<>();
        Optional<Boolean> result = anatomicalArea.map(ar -> {
            List<LSMImage> lsmFiles = ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()).collect(Collectors.toList());
            return lsmFiles;
        }).map(lsmFiles -> {
            List<File> metadataFiles = new ArrayList<>();
            lsmFiles.forEach(lf -> {
                File lsmMetadataFile = getOutputFileName(sampleLsmMetadataArgs.outputDir, lf.getFilepath());
                sampleDataService.updateLMSMetadata(lf, lsmMetadataFile.getAbsolutePath());
                metadataFiles.add(lsmMetadataFile);
            });
            jacsService.setResult(metadataFiles);
            return true;
        });
        if (result.isPresent()) {
            processData.complete(jacsService);
        } else {
            processData.completeExceptionally(new IllegalStateException("Something must have gone wrong because not all sample LSMs had their metadata updated"));
        }
        return processData;
    }

    private CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs getArgs(JacsServiceData jacsServiceData) {
        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs sampleLsmMetadataArgs = new CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs();
        new JCommander(sampleLsmMetadataArgs).parse(jacsServiceData.getArgsArray());
        return sampleLsmMetadataArgs;
    }

    private File getOutputFileName(String outputDir, String inputFileName) {
        File inputFile = new File(inputFileName);
        return new File(outputDir, inputFile.getName().replaceAll("\\s+", "_") + ".json");
    }
}
