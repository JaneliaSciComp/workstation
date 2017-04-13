package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.DefaultServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractAnyServiceResultHandler;
import org.janelia.jacs2.asyncservice.imageservices.BasicMIPsAndMoviesProcessor;
import org.janelia.jacs2.asyncservice.utils.FileUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
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
import java.util.LinkedList;
import java.util.List;

@Named("getSampleMIPsAndMovies")
public class GetSampleMIPsAndMoviesProcessor extends AbstractBasicLifeCycleServiceProcessor<GetSampleMIPsAndMoviesProcessor.GetSampleMIPsIntermediateResult, List<SampleImageMIPsFile>> {

    static class GetSampleMIPsIntermediateResult {
        final Number getSampleLsmsServiceDataId;
        final List<SampleImageMIPsFile> sampleImageFileWithMips = new LinkedList<>();

        public GetSampleMIPsIntermediateResult(Number getSampleLsmsServiceDataId) {
            this.getSampleLsmsServiceDataId = getSampleLsmsServiceDataId;
        }

        public void addSampleImageMipsFile(SampleImageMIPsFile simf) {
            sampleImageFileWithMips.add(simf);
        }
    }

    static class SampleMIPsAndMoviesArgs extends SampleServiceArgs {
        @Parameter(names = "-options", description = "Options", required = false)
        String options = "mips:movies:legends:bcomp";
        @Parameter(names = "-mipsSubDir", description = "MIPs and movies directory relative to sampleData directory", required = false)
        public String mipsSubDir = "mips";
    }

    private final GetSampleImageFilesProcessor getSampleImageFilesProcessor;
    private final BasicMIPsAndMoviesProcessor basicMIPsAndMoviesProcessor;

    @Inject
    GetSampleMIPsAndMoviesProcessor(ServiceComputationFactory computationFactory,
                                    JacsServiceDataPersistence jacsServiceDataPersistence,
                                    @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                    GetSampleImageFilesProcessor getSampleImageFilesProcessor,
                                    BasicMIPsAndMoviesProcessor basicMIPsAndMoviesProcessor,
                                    Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.getSampleImageFilesProcessor = getSampleImageFilesProcessor;
        this.basicMIPsAndMoviesProcessor = basicMIPsAndMoviesProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new SampleMIPsAndMoviesArgs());
    }

    @Override
    public ServiceResultHandler<List<SampleImageMIPsFile>> getResultHandler() {
        return new AbstractAnyServiceResultHandler<List<SampleImageMIPsFile>>() {
            final String resultsPattern = "glob:**/*.{png,avi,mp4}";

            @Override
            public boolean isResultReady(JacsServiceResult<?> depResults) {
                return areAllDependenciesDone(depResults.getJacsServiceData());
            }

            @Override
            public List<SampleImageMIPsFile> collectResult(JacsServiceResult<?> depResults) {
                GetSampleMIPsIntermediateResult result = (GetSampleMIPsIntermediateResult) depResults.getResult();
                result.sampleImageFileWithMips.stream()
                        .forEach(simf -> {
                            Path mipsResultsDir = Paths.get(simf.getMipsResultsDir());
                            FileUtils.lookupFiles(mipsResultsDir, 1, resultsPattern)
                                    .forEach(p -> simf.addMipFile(p.toString()));
                        });
                return result.sampleImageFileWithMips;
            }
        };
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return new DefaultServiceErrorChecker(logger);
    }

    @Override
    protected JacsServiceResult<GetSampleMIPsIntermediateResult> submitServiceDependencies(JacsServiceData jacsServiceData) {
        SampleMIPsAndMoviesArgs args = getArgs(jacsServiceData);

        JacsServiceData getSampleLsmsServiceRef = getSampleImageFilesProcessor.createServiceData(new ServiceExecutionContext(jacsServiceData),
                new ServiceArg("-sampleId", args.sampleId.toString()),
                new ServiceArg("-objective", args.sampleObjective),
                new ServiceArg("-sampleDataDir", args.sampleDataDir)
        );
        JacsServiceData getSampleLsmsService = submitDependencyIfNotPresent(jacsServiceData, getSampleLsmsServiceRef);
        return new JacsServiceResult<>(jacsServiceData, new GetSampleMIPsIntermediateResult(getSampleLsmsService.getId()));
    }

    @Override
    protected ServiceComputation<JacsServiceResult<GetSampleMIPsIntermediateResult>> processing(JacsServiceResult<GetSampleMIPsIntermediateResult> depResults) {
        return computationFactory.newCompletedComputation(depResults)
                .thenApply(pd -> {
                    SampleMIPsAndMoviesArgs args = getArgs(pd.getJacsServiceData());
                    JacsServiceData getSampleLsmsService = jacsServiceDataPersistence.findById(depResults.getResult().getSampleLsmsServiceDataId);
                    List<SampleImageFile> sampleImageFiles = getSampleImageFilesProcessor.getResultHandler().getServiceDataResult(getSampleLsmsService);
                    sampleImageFiles.stream()
                            .forEach(sif -> {
                                String lsmImageFileName = sif.getWorkingFilePath();
                                if (!sif.isChanSpecDefined()) {
                                    throw new ComputationException(pd.getJacsServiceData(), "No channel spec for LSM " + sif.getId());
                                }
                                Path resultsDir =  getResultsDir(args, sif.getArea(), sif.getObjective(), new File(lsmImageFileName));
                                JacsServiceData basicMipMapsService = basicMIPsAndMoviesProcessor.createServiceData(new ServiceExecutionContext.Builder(depResults.getJacsServiceData())
                                                .waitFor(getSampleLsmsService)
                                                .build(),
                                        new ServiceArg("-imgFile", lsmImageFileName),
                                        new ServiceArg("-chanSpec", sif.getChanSpec()),
                                        new ServiceArg("-colorSpec", sif.getColorSpec()),
                                        new ServiceArg("-laser", null), // no laser info in the lsm
                                        new ServiceArg("-gain", null), // no gain info in the lsm
                                        new ServiceArg("-options", args.options),
                                        new ServiceArg("-resultsDir", resultsDir.toString())
                                );
                                submitDependencyIfNotPresent(depResults.getJacsServiceData(), basicMipMapsService);
                                SampleImageMIPsFile sampleImageMIPsFile = new SampleImageMIPsFile();
                                sampleImageMIPsFile.setSampleImageFile(sif);
                                sampleImageMIPsFile.setMipsResultsDir(resultsDir.toString());
                                depResults.getResult().addSampleImageMipsFile(sampleImageMIPsFile);
                            });
                    return pd;
                });
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
