package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.service.dataservice.sample.SampleDataService;
import org.janelia.jacs2.service.impl.AbstractServiceComputation;
import org.janelia.jacs2.service.impl.JacsService;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Named("sampleLsmMetadataService")
public class CreateSampleLsmMetadataComputation extends AbstractServiceComputation<Void> {

    @Inject
    private SampleDataService sampleDataService;

    @Override
    public CompletionStage<JacsService<Void>> preProcessData(JacsService<Void> jacsService) {
        JacsServiceData serviceData = jacsService.getJacsServiceData();
        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs sampleLsmMetadataArgs = getArgs(serviceData);
        Optional<AnatomicalArea> anatomicalArea =
                sampleDataService.getAnatomicalAreaBySampleIdAndObjective(jacsService.getOwner(), sampleLsmMetadataArgs.sampleId, sampleLsmMetadataArgs.sampleObjective);
        if (!anatomicalArea.isPresent()) {
            CompletableFuture<JacsService<Void>> preProcessExc = new CompletableFuture<>();
            preProcessExc.completeExceptionally(new IllegalArgumentException("No anatomical area found for " + serviceData));
            return preProcessExc;
        } else {
            anatomicalArea
                .map(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()).collect(Collectors.toList())).map(lsmFiles -> lsmFiles.stream().map(lf -> {
                    JacsServiceData lsmMetadataServiceData =
                        new JacsServiceDataBuilder(serviceData)
                                .setName("lsmMetadata")
                                .addArg("-inputLSM", lf.getFilepath())
                                .addArg("-outputLSMMetadata", getOutputFileName(sampleLsmMetadataArgs.outputDir, lf.getFilepath()))
                                .build();
                    jacsService.submitChildServiceAsync(lsmMetadataServiceData);
                    return lsmMetadataServiceData.getId();
                }));
            return super.preProcessData(jacsService);
        }
    }

    @Override
    public CompletionStage<JacsService<Void>> processData(JacsService<Void> jacsService) {
        // collect the metadata files and update the corresponding LSMs
        JacsServiceData serviceData = jacsService.getJacsServiceData();
        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs sampleLsmMetadataArgs = getArgs(serviceData);
        Optional<AnatomicalArea> anatomicalArea =
                sampleDataService.getAnatomicalAreaBySampleIdAndObjective(jacsService.getOwner(), sampleLsmMetadataArgs.sampleId, sampleLsmMetadataArgs.sampleObjective);
        CompletableFuture<JacsService<Void>> processData = new CompletableFuture<>();
        Optional<Boolean> result = anatomicalArea.map(ar -> {
            List<LSMImage> lsmFiles = ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()).collect(Collectors.toList());
            return lsmFiles;
        }).map(lsmFiles -> {
            lsmFiles.forEach(lf -> {
                String lsmMetadataFile = getOutputFileName(sampleLsmMetadataArgs.outputDir, lf.getFilepath());
                sampleDataService.updateLMSMetadata(lf, lsmMetadataFile);
            });
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
        new JCommander(sampleLsmMetadataArgs).parse(jacsServiceData.getArgsAsArray());
        return sampleLsmMetadataArgs;
    }

    private String getOutputFileName(String outputDir, String inputFileName) {
        File inputFile = new File(inputFileName);
        return new File(outputDir, inputFile.getName().replaceAll("\\s+", "_") + ".json").getAbsolutePath();
    }
}
