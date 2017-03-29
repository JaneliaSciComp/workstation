package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.DefaultServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractFileListServiceResultHandler;
import org.janelia.jacs2.asyncservice.imageservices.BasicMIPsAndMoviesProcessor;
import org.janelia.jacs2.asyncservice.utils.FileUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.sample.SampleDataService;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.nio.file.Path;
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
    GetSampleMIPsAndMoviesProcessor(ServiceComputationFactory computationFactory,
                                    JacsServiceDataPersistence jacsServiceDataPersistence,
                                    @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                    SampleDataService sampleDataService,
                                    GetSampleImageFilesProcessor getSampleImageFilesProcessor,
                                    BasicMIPsAndMoviesProcessor basicMIPsAndMoviesProcessor,
                                    Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.sampleDataService = sampleDataService;
        this.getSampleImageFilesProcessor = getSampleImageFilesProcessor;
        this.basicMIPsAndMoviesProcessor = basicMIPsAndMoviesProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new SampleMIPsAndMoviesArgs());
    }

    @Override
    public ServiceResultHandler<List<File>> getResultHandler() {
        return new AbstractFileListServiceResultHandler() {
            final String resultsPattern = "glob:**/*.{png,avi,mp4}";

            @Override
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                return areAllDependenciesDone(jacsServiceData);
            }

            @Override
            public List<File> collectResult(JacsServiceData jacsServiceData) {
                SampleMIPsAndMoviesArgs args = getArgs(jacsServiceData);
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
                            FileUtils.lookupFiles(resultsDir, 1, resultsPattern).forEach(p -> results.add(p.toFile()));
                        });
                return results;
            }
        };
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return new DefaultServiceErrorChecker(logger);
    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        SampleMIPsAndMoviesArgs args = getArgs(jacsServiceData);

        JacsServiceData jacsServiceDataHierarchy = jacsServiceDataPersistence.findServiceHierarchy(jacsServiceData.getId());

        JacsServiceData getSampleLsmsServiceRef = getSampleImageFilesProcessor.createServiceData(new ServiceExecutionContext(jacsServiceData),
                new ServiceArg("-sampleId", args.sampleId.toString()),
                new ServiceArg("-objective", args.sampleObjective),
                new ServiceArg("-sampleDataDir", args.sampleDataDir)
        );
        JacsServiceData getSampleLsmsService = submitDependencyIfNotPresent(jacsServiceDataHierarchy, getSampleLsmsServiceRef);

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
                            JacsServiceData basicMipMapsService = basicMIPsAndMoviesProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                                            .waitFor(getSampleLsmsService)
                                            .build(),
                                    new ServiceArg("-imgFile", lsmImageFile.getAbsolutePath()),
                                    new ServiceArg("-chanSpec", lsmf.getChanSpec()),
                                    new ServiceArg("-colorSpec", lsmf.getChannelColors()),
                                    new ServiceArg("-laser", null), // no laser info in the lsm
                                    new ServiceArg("-gain", null), // no gain info in the lsm
                                    new ServiceArg("-options", args.options),
                                    new ServiceArg("-resultsDir", resultsDir.toString())
                            );
                            return submitDependencyIfNotPresent(jacsServiceDataHierarchy, basicMipMapsService);
                        }))
                .collect(Collectors.toList());
    }

    @Override
    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(jacsServiceData);
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
