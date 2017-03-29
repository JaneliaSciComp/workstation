package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractSingleFileServiceResultHandler;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Named("stitchGrouping")
public class StitchGroupingProcessor extends AbstractBasicLifeCycleServiceProcessor<File> {

    static class StitchGroupingArgs extends ServiceArgs {
        @Parameter(names = "-referenceChannelIndex", description = "Reference channel index", required = true)
        int referenceChannelIndex;
        @Parameter(names = "-inputDir", description = "Input directory path", required = true)
        String inputDir;
        @Parameter(names = "-resultDir", description = "Result directory", required = true)
        String resultDir;
    }

    private static final String DEFAULT_GROUP_RESULT_FILE_NAME = "igroups.txt";

    private final Vaa3dPluginProcessor vaa3dPluginProcessor;

    @Inject
    StitchGroupingProcessor(ServiceComputationFactory computationFactory,
                            JacsServiceDataPersistence jacsServiceDataPersistence,
                            @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                            Vaa3dPluginProcessor vaa3dPluginProcessor,
                            Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dPluginProcessor= vaa3dPluginProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new StitchGroupingArgs());
    }

    @Override
    public ServiceResultHandler<File> getResultHandler() {
        return new AbstractSingleFileServiceResultHandler() {

            @Override
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                File outputFile = getGroupResultFile(jacsServiceData);
                return outputFile.exists();
            }

            @Override
            public File collectResult(JacsServiceData jacsServiceData) {
                return getGroupResultFile(jacsServiceData);
            }
        };
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return vaa3dPluginProcessor.getErrorChecker();
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            StitchGroupingArgs  args = getArgs(jacsServiceData);
            Files.createDirectories(Paths.get(args.resultDir));
        } catch (Exception e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return jacsServiceData;
    }

    @Override
    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        StitchGroupingArgs args = getArgs(jacsServiceData);
        JacsServiceData vaa3dPluginService = createVaa3dPluginService(args, jacsServiceData);
        return vaa3dPluginProcessor.process(vaa3dPluginService)
                .thenApply(voidResult -> jacsServiceData);
    }

    private JacsServiceData createVaa3dPluginService(StitchGroupingArgs args, JacsServiceData jacsServiceData) {
        return vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .setErrorPath(jacsServiceData.getErrorPath())
                        .setOutputPath(jacsServiceData.getOutputPath())
                        .state(JacsServiceState.RUNNING).build(),
                new ServiceArg("-plugin", "imageStitch.so"),
                new ServiceArg("-pluginFunc", "istitch-grouping"),
                new ServiceArg("-input", args.inputDir),
                new ServiceArg("-output", args.resultDir),
                new ServiceArg("-pluginParams", String.format("#c %d", args.referenceChannelIndex))
        );
    }

    private StitchGroupingArgs getArgs(JacsServiceData jacsServiceData) {
        return StitchGroupingArgs.parse(jacsServiceData.getArgsArray(), new StitchGroupingArgs());
    }

    private File getGroupResultFile(JacsServiceData jacsServiceData) {
        StitchGroupingArgs args = getArgs(jacsServiceData);
        return new File(args.resultDir, DEFAULT_GROUP_RESULT_FILE_NAME);
    }

}
