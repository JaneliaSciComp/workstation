package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.imageservices.BasicMIPsAndMoviesProcessor;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.sample.SampleDataService;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Named("getSampleMIPsAndMovies")
public class GetSampleMIPsAndMoviesProcessor extends AbstractBasicLifeCycleServiceProcessor<List<File>> {

    static class SampleMIPsAndMoviesArgs extends SampleServiceArgs {
        @Parameter(names = "-options", description = "Options", required = false)
        String options = "mips:movies:legends:bcomp";
        @Parameter(names = "-mipsSubDir", description = "MIPs and movies directory relative to sampleData directory", required = false)
        public String mipsSubDir = "mips";
    }

    private final SampleDataService sampleDataService;
    private final GetSampleImageFilesProcessor getSampleImageFilesProcessor;
    private final BasicMIPsAndMoviesProcessor basicMIPsAndMoviesProcessor;

    @Inject
    GetSampleMIPsAndMoviesProcessor(JacsServiceEngine jacsServiceEngine,
                                    ServiceComputationFactory computationFactory,
                                    JacsServiceDataPersistence jacsServiceDataPersistence,
                                    @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                    SampleDataService sampleDataService,
                                    Logger logger,
                                    GetSampleImageFilesProcessor getSampleImageFilesProcessor,
                                    BasicMIPsAndMoviesProcessor basicMIPsAndMoviesProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.sampleDataService = sampleDataService;
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
    protected ServiceComputation<JacsServiceData> prepareProcessing(JacsServiceData jacsServiceData) {
        return createComputation(jacsServiceData);
    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        SampleMIPsAndMoviesArgs args = getArgs(jacsServiceData);

        JacsServiceData getSampleLsms = submit(getSampleImageFilesProcessor.createServiceData(new ServiceExecutionContext(jacsServiceData),
                new ServiceArg("-sampleId", args.sampleId.toString()),
                new ServiceArg("-objective", args.sampleObjective),
                new ServiceArg("-sampleDataDir", args.sampleDataDir)));

        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsServiceData.getOwner(), args.sampleId, args.sampleObjective);

        return anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs()
                        .stream()
                        .flatMap(lsmp -> lsmp.getLsmFiles().stream())
                        .map(lsmf -> {
                            File lsmImageFile = SampleServicesUtils.getImageFile(Paths.get(args.sampleDataDir), lsmf);
                            if (!lsmf.isChanSpecDefined()) {
                                throw new ComputationException(jacsServiceData, "No channel spec for LSM " + lsmf.getId());
                            }
                            Path resultsDir =  getResultsDir(args, ar.getName(), ar.getObjective(), lsmImageFile);
                            return submit(basicMIPsAndMoviesProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                                            .waitFor(getSampleLsms)
                                            .build(),
                                    new ServiceArg("-imgFile", lsmImageFile.getAbsolutePath()),
                                    new ServiceArg("-chanSpec", lsmf.getChanSpec()),
                                    new ServiceArg("-colorSpec", lsmf.getChannelColors()),
                                    new ServiceArg("-laser", null), // no laser info in the lsm
                                    new ServiceArg("-gain", null), // no gain info in the lsm
                                    new ServiceArg("-options", args.options),
                                    new ServiceArg("-resultsDir", resultsDir.toString())));
                        }))
                .collect(Collectors.toList());
    }

    protected ServiceComputation<List<File>> processing(JacsServiceData jacsServiceData) {
        return createComputation(this.waitForResult(jacsServiceData));
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        return checkForDependenciesCompletion(jacsServiceData);
    }

    @Override
    protected List<File> retrieveResult(JacsServiceData jacsServiceData) {
        SampleMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        // collect all AVIs, PNGs and MP4s
        String resultsPattern = "glob:**/*.{png,avi,mp4}";
        PathMatcher inputFileMatcher =
                FileSystems.getDefault().getPathMatcher(resultsPattern);
        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsServiceData.getOwner(), args.sampleId, args.sampleObjective);

        List<File> results = new ArrayList<>();
        anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs()
                        .stream()
                        .flatMap(lsmp -> lsmp.getLsmFiles().stream())
                        .map(lsmf -> {
                            File lsmImageFile = SampleServicesUtils.getImageFile(Paths.get(args.sampleDataDir), lsmf);

                            if (!lsmf.isChanSpecDefined()) {
                                throw new ComputationException(jacsServiceData, "No channel spec for LSM " + lsmf.getId());
                            }
                            return getResultsDir(args, ar.getName(), ar.getObjective(), lsmImageFile);
                        }))
                .forEach(resultsDir -> {
                    try {
                        Files.find(resultsDir, 1, (p, a) -> inputFileMatcher.matches(p)).forEach(p -> results.add(p.toFile()));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        return results;
    }

    private Path getResultsDir(SampleMIPsAndMoviesArgs args, String area, String objective, File lsmImageFile) {
        ImmutableList.Builder<String> pathCompBuilder = new ImmutableList.Builder<>();
        if (StringUtils.isNotBlank(objective)) {
            pathCompBuilder.add(objective);
        }
        if (StringUtils.isNotBlank(area)) {
            pathCompBuilder.add(area);
        }
        pathCompBuilder.add(com.google.common.io.Files.getNameWithoutExtension(lsmImageFile.getAbsolutePath()));
        pathCompBuilder.add(args.mipsSubDir);
        ImmutableList<String> pathComps = pathCompBuilder.build();
        return Paths.get(args.sampleDataDir, pathComps.toArray(new String[pathComps.size()]));
    }

    private SampleMIPsAndMoviesArgs getArgs(JacsServiceData jacsServiceData) {
        return SampleServiceArgs.parse(jacsServiceData.getArgsArray(), new SampleMIPsAndMoviesArgs());
    }
}
