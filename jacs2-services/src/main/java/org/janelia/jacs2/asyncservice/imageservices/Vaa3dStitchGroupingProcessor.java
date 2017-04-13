package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Named("vaa3dStitchGrouping")
public class Vaa3dStitchGroupingProcessor extends AbstractBasicLifeCycleServiceProcessor<File> {

    private static final String GROUPS_FILENAME = "igroups.txt";

    static class Vaa3dStitchGroupingArgs extends ServiceArgs {
        @Parameter(names = "-inputDir", description = "Input directory", required = true)
        String inputDir;
        @Parameter(names = "-outputDir", description = "Output directory", required = true)
        String outputDir;
        @Parameter(names = "-refchannel", description = "Reference channel")
        int referenceChannel = 4;
        @Parameter(names = {"-p", "-pluginParams"}, description = "Other plugin parameters")
        List<String> pluginParams = new ArrayList<>();
    }

    private final Vaa3dPluginProcessor vaa3dPluginProcessor;

    @Inject
    Vaa3dStitchGroupingProcessor(ServiceComputationFactory computationFactory,
                                 JacsServiceDataPersistence jacsServiceDataPersistence,
                                 @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                 Vaa3dPluginProcessor vaa3dPluginProcessor,
                                 Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Vaa3dStitchGroupingArgs());
    }

    @Override
    public ServiceResultHandler<File> getResultHandler() {
        return new AbstractSingleFileServiceResultHandler() {
            @Override
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                return getGroupsFile(getArgs(jacsServiceData)).toFile().exists();
            }

            @Override
            public File collectResult(JacsServiceData jacsServiceData) {
                return getGroupsFile(getArgs(jacsServiceData)).toFile();
            }
        };
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return vaa3dPluginProcessor.getErrorChecker();
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        Vaa3dStitchGroupingArgs args = getArgs(jacsServiceData);
        try {
            Files.createDirectories(getOutputDir(args));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        Vaa3dStitchGroupingArgs args = getArgs(jacsServiceData);
        JacsServiceData vaa3dPluginService = createVaa3dPluginService(args, jacsServiceData);
        return vaa3dPluginProcessor.process(vaa3dPluginService)
                .thenApply(voidResult -> jacsServiceData);
    }

    private JacsServiceData createVaa3dPluginService(Vaa3dStitchGroupingArgs args, JacsServiceData jacsServiceData) {
        return vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .setServiceName(jacsServiceData.getName())
                        .setErrorPath(jacsServiceData.getErrorPath())
                        .setOutputPath(jacsServiceData.getOutputPath())
                        .state(JacsServiceState.RUNNING).build(),
                new ServiceArg("-plugin", "imageStitch.so"),
                new ServiceArg("-pluginFunc", "istitch-grouping"),
                new ServiceArg("-input", args.inputDir),
                new ServiceArg("-output", args.outputDir),
                new ServiceArg("-pluginParams", String.format("#c %d", args.referenceChannel)),
                new ServiceArg("-pluginParams", String.join(",", args.pluginParams))
        );
    }

    private Vaa3dStitchGroupingArgs getArgs(JacsServiceData jacsServiceData) {
        return ServiceArgs.parse(jacsServiceData.getArgsArray(), new Vaa3dStitchGroupingArgs());
    }

    private Path getOutputDir(Vaa3dStitchGroupingArgs args) {
        return Paths.get(args.outputDir);
    }

    private Path getGroupsFile(Vaa3dStitchGroupingArgs args) {
        Path outputDir = getOutputDir(args);
        return outputDir.resolve(GROUPS_FILENAME);
    }
}
