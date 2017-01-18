package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import com.google.common.base.Splitter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.lsmfileservices.ArchivedLsmMetadataServiceDescriptor;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.model.service.JacsServiceState;
import org.janelia.jacs2.model.service.ProcessingLocation;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.service.dataservice.sample.SampleDataService;
import org.janelia.jacs2.service.impl.AbstractServiceProcessor;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.JacsServiceDispatcher;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceComputationFactory;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class CreateSampleLsmMetadataProcessor extends AbstractServiceProcessor<List<File>> {

    private final SampleDataService sampleDataService;

    @Inject
    public CreateSampleLsmMetadataProcessor(JacsServiceDispatcher jacsServiceDispatcher,
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
        if (StringUtils.isNotBlank(jacsServiceData.getStringifiedResult())) {
            return Splitter.on(",").omitEmptyStrings().trimResults()
                    .splitToList(jacsServiceData.getStringifiedResult())
                    .stream()
                    .map(File::new)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void setResult(List<File> result, JacsServiceData jacsServiceData) {
        if (CollectionUtils.isNotEmpty(result)) {
            jacsServiceData.setStringifiedResult(result.stream()
                    .filter(r -> r != null)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(",")));
        } else {
            jacsServiceData.setStringifiedResult(null);
        }
    }

    @Override
    protected ServiceComputation<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs args = getArgs(jacsServiceData);
        if (args.sampleId == null) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "Sample Id is required"));
        } else if (StringUtils.isBlank(args.outputDir)) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "Output directory is required"));
        }
        return submitChildService(jacsServiceData,
                new JacsServiceDataBuilder(jacsServiceData)
                        .setName("sampleImageFiles")
                        .addArg("-sampleId", args.sampleId.toString())
                        .addArg("-objective", args.sampleObjective)
                        .addArg("-dest", args.outputDir)
                        .build());
    }

    @Override
    protected ServiceComputation<List<File>> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs args = getArgs(jacsServiceData);
        JacsServiceData sampleImageFilesData = (JacsServiceData) preProcessingResult;
        // wait for the child services to complete and set the result to the result of the last computation (LSM metadata)
        return this.waitForCompletion(sampleImageFilesData)
                .thenApply(r -> {
                    List<ServiceComputation<?>> lsmc = new ArrayList<>();
                    List<File> imageFiles = (List<File>) r;
                    imageFiles.forEach(f -> {
                        File lsmMetadataFile = getOutputFileName(args.outputDir, f);
                        JacsServiceData extractLsmMetadataService =
                                new JacsServiceDataBuilder(jacsServiceData)
                                        .setName("lsmFileMetadata")
                                        .addArg("-inputLSM", f.getAbsolutePath())
                                        .addArg("-outputLSMMetadata", lsmMetadataFile.getAbsolutePath())
                                        .build();
                        lsmc.add(this.submitChildService(jacsServiceData, extractLsmMetadataService));
                    });
                    return computationFactory.newCompletedComputation(lsmc);
                })
                .thenApply(lr -> (List<File>) lr);
    }

    //    @Override
//    public CompletionStage<JacsService<List<File>>> preProcessData(JacsService<List<File>> jacsService) {
//        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs args = getArgs(jacsService);
//        List<AnatomicalArea> anatomicalAreas =
//                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsService.getOwner(), args.sampleId, args.sampleObjective);
//        if (anatomicalAreas.isEmpty()) {
//            CompletableFuture<JacsService<List<File>>> preProcessExc = new CompletableFuture<>();
//            preProcessExc.completeExceptionally(new IllegalArgumentException("No anatomical areas found for " +
//                    args.sampleId +
//                    (StringUtils.isBlank(args.sampleObjective) ? "" : args.sampleObjective)));
//            return preProcessExc;
//        }
//        List<Number> result = anatomicalAreas.stream()
//                .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
//                .map(lf -> {
//                    File lsmMetadataFileName = getOutputFileName(args.outputDir, lf.getFilepath());
//                    JacsServiceDataBuilder lsmMetadataServiceDataBuilder =
//                            new JacsServiceDataBuilder(jacsService.getJacsServiceData())
//                                    .setName("archivedLsmMetadata")
//                                    .addArg("-archivedLSM", lf.getFilepath())
//                                    .addArg("-outputLSMMetadata", lsmMetadataFileName.getAbsolutePath());
//                    JacsServiceData lsmMetadataServiceData = lsmMetadataServiceDataBuilder.build();
//                    jacsService.submitChildServiceAsync(lsmMetadataServiceData);
//                    return lsmMetadataServiceData.getId();
//                }).collect(Collectors.toList());
//        if (result.isEmpty()) {
//            jacsService.setState(JacsServiceState.CANCELED);
//            CompletableFuture<JacsService<List<File>>> preProcessExc = new CompletableFuture<>();
//            preProcessExc.completeExceptionally(new IllegalArgumentException("No LSM image found for " +
//                    args.sampleId +
//                    (StringUtils.isBlank(args.sampleObjective) ? "" : args.sampleObjective)));
//            return preProcessExc;
//        }
//        logger.info("Created child services {} to extract metadata from the LSMs for {}", result, jacsService.getJacsServiceData());
//        return super.preProcessData(jacsService);
//    }

//    @Override
//    public CompletionStage<JacsService<List<File>>> processData(JacsService<List<File>> jacsService) {
//        // collect the metadata files and update the corresponding LSMs
//        CompletableFuture<JacsService<List<File>>> processData = new CompletableFuture<>();
//        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs args = getArgs(jacsService);
//        List<AnatomicalArea> anatomicalAreas =
//                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsService.getOwner(), args.sampleId, args.sampleObjective);
//        List<File> metadataFiles = new ArrayList<>();
//        anatomicalAreas.stream()
//                .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
//                .forEach(lf -> {
//                    File lsmMetadataFile = getOutputFileName(args.outputDir, lf.getFilepath());
//                    sampleDataService.updateLMSMetadata(lf, lsmMetadataFile.getAbsolutePath());
//                    metadataFiles.add(lsmMetadataFile);
//                });
//        if (metadataFiles.isEmpty()) {
//            processData.completeExceptionally(new IllegalStateException("Something must have gone wrong because not all sample LSMs had their metadata updated"));
//        } else {
//            jacsService.setResult(metadataFiles);
//            processData.complete(jacsService);
//        }
//        return processData;
//    }

    private CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs getArgs(JacsServiceData jacsServiceData) {
        CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs sampleLsmMetadataArgs = new CreateSampleLsmMetadataServiceDescriptor.SampleLsmMetadataArgs();
        new JCommander(sampleLsmMetadataArgs).parse(jacsServiceData.getArgsArray());
        return sampleLsmMetadataArgs;
    }

    private File getOutputFileName(String outputDir, File inputFile) {
        return new File(outputDir, inputFile.getName().replaceAll("\\s+", "_") + ".json");
    }
}
