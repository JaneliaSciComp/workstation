package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.imageservices.BasicMIPsAndMoviesProcessor;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Named("getSampleMIPsAndMovies")
public class GetSampleMIPsAndMoviesProcessor extends AbstractServiceProcessor<List<File>> {

    static class SampleMIPsAndMoviesArgs extends SampleServiceArgs {
        @Parameter(names = "-options", description = "Options", required = false)
        String options = "mips:movies:legends:bcomp";
        @Parameter(names = "-mipsSubDir", description = "MIPs and movies directory relative to sampleData directory", required = false)
        public String mipsSubDir = "mips";
    }

    private final GetSampleImageFilesProcessor getSampleImageFilesProcessor;
    private final BasicMIPsAndMoviesProcessor basicMIPsAndMoviesProcessor;

    @Inject
    GetSampleMIPsAndMoviesProcessor(JacsServiceEngine jacsServiceEngine,
                                    ServiceComputationFactory computationFactory,
                                    JacsServiceDataPersistence jacsServiceDataPersistence,
                                    @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                    Logger logger,
                                    GetSampleImageFilesProcessor getSampleImageFilesProcessor,
                                    BasicMIPsAndMoviesProcessor basicMIPsAndMoviesProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.getSampleImageFilesProcessor = getSampleImageFilesProcessor;
        this.basicMIPsAndMoviesProcessor = basicMIPsAndMoviesProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new SampleMIPsAndMoviesArgs());
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
    protected ServiceComputation<List<SampleImageFile>> preProcessData(JacsServiceData jacsServiceData) {
        SampleServiceArgs args = getArgs(jacsServiceData);
        return getSampleImageFilesProcessor.invokeAsync(new ServiceExecutionContext(jacsServiceData),
                new ServiceArg("-sampleId", args.sampleId.toString()),
                new ServiceArg("-objective", args.sampleObjective),
                new ServiceArg("-sampleDataDir", args.sampleDataDir))
                .thenCompose(this::waitForCompletion)
                .thenApply(getSampleImageFilesProcessor::getResult);
    }

    @Override
    protected ServiceComputation<List<File>> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        List<SampleImageFile> sampleLSMs = (List<SampleImageFile>) preProcessingResult;
        if (CollectionUtils.isEmpty(sampleLSMs)) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "No sample image file was found"));
        }
        SampleMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        List<ServiceComputation<?>> mipsComputations = submitAllBasicMipsAndMoviesServices(sampleLSMs, args, jacsServiceData);
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenCombineAll(mipsComputations, (sd, basicMipsResults) -> {
                    List<File> sampleMIPsResults = new ArrayList<>();
                    basicMipsResults.forEach(f -> sampleMIPsResults.addAll((List<File>) f));
                    return sampleMIPsResults;
                });
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<File> retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        throw new UnsupportedOperationException();
    }

    private List<ServiceComputation<?>> submitAllBasicMipsAndMoviesServices(List<SampleImageFile> lsmFiles, SampleMIPsAndMoviesArgs args, JacsServiceData jacsServiceData) {
        List<ServiceComputation<?>> basicMipsAndMoviesComputations = new ArrayList<>();
        lsmFiles.forEach(f -> {
            if (!f.isChanSpecDefined()) {
                throw new ComputationException(jacsServiceData,
                        "No channel spec for LSM " + f.getId() + "-" + f.getArchiveFilePath());
            }
            Path resultsDir = getResultsDir(args, f);
            ServiceComputation<?> basicMipsAndMoviesComputation = basicMIPsAndMoviesProcessor.invokeAsync(new ServiceExecutionContext(jacsServiceData),
                    new ServiceArg("-imgFile", f.getWorkingFilePath()),
                    new ServiceArg("-chanSpec", f.getChanSpec()),
                    new ServiceArg("-colorSpec", f.getColorSpec()),
                    new ServiceArg("-laser", f.getLaser() == null ? null : f.getLaser().toString()),
                    new ServiceArg("-gain", f.getGain() == null ? null : f.getGain().toString()),
                    new ServiceArg("-options", args.options),
                    new ServiceArg("-resultsDir", resultsDir.toString()))
                    .thenCompose(this::waitForCompletion)
                    .thenApply(basicMIPsAndMoviesProcessor::getResult);
            basicMipsAndMoviesComputations.add(basicMipsAndMoviesComputation);
        });
        return basicMipsAndMoviesComputations;
    }

    private Path getResultsDir(SampleMIPsAndMoviesArgs args, SampleImageFile sampleImgFile) {
        ImmutableList.Builder<String> pathCompBuilder = new ImmutableList.Builder<>();
        if (StringUtils.isNotBlank(sampleImgFile.getObjective())) {
            pathCompBuilder.add(sampleImgFile.getObjective());
        }
        if (StringUtils.isNotBlank(sampleImgFile.getArea())) {
            pathCompBuilder.add(sampleImgFile.getArea());
        }
        pathCompBuilder.add(com.google.common.io.Files.getNameWithoutExtension(sampleImgFile.getWorkingFilePath()));
        pathCompBuilder.add(args.mipsSubDir);
        ImmutableList<String> pathComps = pathCompBuilder.build();
        return Paths.get(args.sampleDataDir, pathComps.toArray(new String[pathComps.size()]));
    }

    private SampleMIPsAndMoviesArgs getArgs(JacsServiceData jacsServiceData) {
        return SampleServiceArgs.parse(jacsServiceData.getArgsArray(), new SampleMIPsAndMoviesArgs());
    }
}
