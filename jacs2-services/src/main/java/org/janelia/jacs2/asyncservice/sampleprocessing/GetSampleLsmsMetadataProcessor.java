package org.janelia.jacs2.asyncservice.sampleprocessing;

import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.DefaultServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractAnyServiceResultHandler;
import org.janelia.jacs2.asyncservice.lsmfileservices.LsmFileMetadataProcessor;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

@Named("getSampleLsmMetadata")
public class GetSampleLsmsMetadataProcessor extends AbstractBasicLifeCycleServiceProcessor<GetSampleLsmsMetadataProcessor.GetSampleLsmsMetadataIntermediateResult, List<SampleImageMetadataFile>> {

    static class GetSampleLsmsMetadataIntermediateResult {
        final Number getSampleLsmsServiceDataId;
        final List<SampleImageMetadataFile> sampleImageFileWithMetadata = new LinkedList<>();

        public GetSampleLsmsMetadataIntermediateResult(Number getSampleLsmsServiceDataId) {
            this.getSampleLsmsServiceDataId = getSampleLsmsServiceDataId;
        }

        public void addSampleImageMetadataFile(SampleImageMetadataFile simf) {
            sampleImageFileWithMetadata.add(simf);
        }
    }

    private final GetSampleImageFilesProcessor getSampleImageFilesProcessor;
    private final LsmFileMetadataProcessor lsmFileMetadataProcessor;

    @Inject
    GetSampleLsmsMetadataProcessor(ServiceComputationFactory computationFactory,
                                   JacsServiceDataPersistence jacsServiceDataPersistence,
                                   @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                   GetSampleImageFilesProcessor getSampleImageFilesProcessor,
                                   LsmFileMetadataProcessor lsmFileMetadataProcessor,
                                   Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.getSampleImageFilesProcessor = getSampleImageFilesProcessor;
        this.lsmFileMetadataProcessor = lsmFileMetadataProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new SampleServiceArgs());
    }

    @Override
    public ServiceResultHandler<List<SampleImageMetadataFile>> getResultHandler() {
        return new AbstractAnyServiceResultHandler<List<SampleImageMetadataFile>>() {
            @Override
            public boolean isResultReady(JacsServiceResult<?> depResults) {
                return areAllDependenciesDone(depResults.getJacsServiceData());
            }

            @Override
            public List<SampleImageMetadataFile> collectResult(JacsServiceResult<?> depResults) {
                GetSampleLsmsMetadataIntermediateResult result = (GetSampleLsmsMetadataIntermediateResult) depResults.getResult();
                return result.sampleImageFileWithMetadata;
            }
        };
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return new DefaultServiceErrorChecker(logger);
    }

    @Override
    protected JacsServiceResult<GetSampleLsmsMetadataIntermediateResult> submitServiceDependencies(JacsServiceData jacsServiceData) {
        SampleServiceArgs args = getArgs(jacsServiceData);

        JacsServiceData getSampleLsmsServiceRef = getSampleImageFilesProcessor.createServiceData(new ServiceExecutionContext(jacsServiceData),
                new ServiceArg("-sampleId", args.sampleId.toString()),
                new ServiceArg("-objective", args.sampleObjective),
                new ServiceArg("-sampleDataDir", args.sampleDataDir)
        );
        JacsServiceData getSampleLsmsService = submitDependencyIfNotPresent(jacsServiceData, getSampleLsmsServiceRef);
        return new JacsServiceResult<>(jacsServiceData, new GetSampleLsmsMetadataIntermediateResult(getSampleLsmsService.getId()));
    }

    @Override
    protected ServiceComputation<JacsServiceResult<GetSampleLsmsMetadataIntermediateResult>> processing(JacsServiceResult<GetSampleLsmsMetadataIntermediateResult> depResults) {
        return computationFactory.newCompletedComputation(depResults)
                .thenApply(pd -> {
                    SampleServiceArgs args = getArgs(pd.getJacsServiceData());
                    JacsServiceData getSampleLsmsService = jacsServiceDataPersistence.findById(depResults.getResult().getSampleLsmsServiceDataId);
                    List<SampleImageFile> sampleImageFiles = getSampleImageFilesProcessor.getResultHandler().getServiceDataResult(getSampleLsmsService);
                    sampleImageFiles.stream()
                            .forEach(sif -> {
                                File lsmImageFile = new File(sif.getWorkingFilePath());
                                File lsmMetadataFile = SampleServicesUtils.getImageMetadataFile(args.sampleDataDir, lsmImageFile);

                                JacsServiceData lsmMetadataService = lsmFileMetadataProcessor.createServiceData(new ServiceExecutionContext.Builder(depResults.getJacsServiceData())
                                            .waitFor(getSampleLsmsService)
                                            .build(),
                                    new ServiceArg("-inputLSM", lsmImageFile.getAbsolutePath()),
                                    new ServiceArg("-outputLSMMetadata", lsmMetadataFile.getAbsolutePath())
                                );
                                submitDependencyIfNotPresent(depResults.getJacsServiceData(), lsmMetadataService);
                                SampleImageMetadataFile sampleImageMetadataFile = new SampleImageMetadataFile();
                                sampleImageMetadataFile.setSampleImageFile(sif);
                                sampleImageMetadataFile.setMetadataFilePath(lsmMetadataFile.getAbsolutePath());
                                depResults.getResult().addSampleImageMetadataFile(sampleImageMetadataFile);
                            });
                    return pd;
                });
    }

    private SampleServiceArgs getArgs(JacsServiceData jacsServiceData) {
        return SampleServiceArgs.parse(jacsServiceData.getArgsArray(), new SampleServiceArgs());
    }

}
