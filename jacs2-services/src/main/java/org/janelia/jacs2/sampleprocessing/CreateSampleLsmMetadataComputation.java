package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.service.dataservice.sample.SampleDataService;
import org.janelia.jacs2.service.impl.AbstractExternalProcessComputation;
import org.janelia.jacs2.service.impl.AbstractServiceComputation;
import org.janelia.jacs2.service.impl.ExternalProcessRunner;
import org.janelia.jacs2.service.impl.ServiceComputation;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named("sampleLsmMetadataService")
public class CreateSampleLsmMetadataComputation extends AbstractServiceComputation {

    static class SampleLsmMetadataArgs {
        @Parameter(names = "-sampleId", description = "Sample ID", required = true)
        Long sampleId;
        @Parameter(names = "-objective", description = "Sample objective", required = true)
        String sampleObjective;
        @Parameter(names = "-outputDir", description = "Destination directory", required = true)
        String outputDir;
    }

    @Inject
    private SampleDataService sampleDataService;

    @Override
    public CompletionStage<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        SampleLsmMetadataArgs sampleLsmMetadataArgs = getArgs(jacsServiceData);
        Optional<AnatomicalArea> anatomicalArea =
                sampleDataService.getAnatomicalAreaBySampleIdAndObjective(jacsServiceData.getOwner(), sampleLsmMetadataArgs.sampleId, sampleLsmMetadataArgs.sampleObjective);
        if (!anatomicalArea.isPresent()) {
            CompletableFuture<JacsServiceData> preProcessExc = new CompletableFuture<>();
            preProcessExc.completeExceptionally(new IllegalArgumentException("No anatomical area found for " + jacsServiceData));
            return preProcessExc;
        } else {
            anatomicalArea
                .map(ar -> {
                    List<LSMImage> lsmFiles = ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()).collect(Collectors.toList());
                    return lsmFiles;
                }).map(lsmFiles -> {
                    return lsmFiles.stream().map(lf -> {
                        JacsServiceData serviceData =
                            new JacsServiceDataBuilder(jacsServiceData)
                                    .addArg("-inputLSM", lf.getFilepath())
                                    .addArg("-outputLSMMetadata", getOutputFileName(sampleLsmMetadataArgs.outputDir, lf.getFilepath()))
                                    .build();
                        submitChildServiceAsync(serviceData, jacsServiceData);
                        return serviceData.getId();
                    });
                });
            return super.preProcessData(jacsServiceData);
        }
    }

    @Override
    public CompletionStage<JacsServiceData> processData(JacsServiceData jacsServiceData) {
        // collect the metadata files and update the corresponding LSMs
        SampleLsmMetadataArgs sampleLsmMetadataArgs = getArgs(jacsServiceData);
        Optional<AnatomicalArea> anatomicalArea =
                sampleDataService.getAnatomicalAreaBySampleIdAndObjective(jacsServiceData.getOwner(), sampleLsmMetadataArgs.sampleId, sampleLsmMetadataArgs.sampleObjective);
        CompletableFuture<JacsServiceData> processData = new CompletableFuture<>();
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
            processData.complete(jacsServiceData);
        } else {
            processData.completeExceptionally(new IllegalStateException("Something must have gone wrong because not all sample LSMs had their metadata updated"));
        }
        return processData;
    }

    private SampleLsmMetadataArgs getArgs(JacsServiceData jacsServiceData) {
        SampleLsmMetadataArgs sampleLsmMetadataArgs = new SampleLsmMetadataArgs();
        new JCommander(sampleLsmMetadataArgs).parse(jacsServiceData.getArgsAsArray());
        return sampleLsmMetadataArgs;
    }

    private String getOutputFileName(String outputDir, String inputFileName) {
        File inputFile = new File(inputFileName);
        return new File(outputDir, inputFile.getName().replaceAll("\\s+", "_") + ".json").getAbsolutePath();
    }

}
