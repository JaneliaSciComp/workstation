package org.janelia.jacs2.asyncservice.imageservices;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Named("vaa3dBlend")
public class Vaa3dBlendProcessor extends AbstractBasicLifeCycleServiceProcessor<File> {

    static class Vaa3dBlendArgs extends ServiceArgs {
        @Parameter(names = "-inputDir", description = "Input file", required = true)
        String inputDir;
        @Parameter(names = "-outputFile", description = "Output file")
        String outputFile;
        @Parameter(names = {"-p", "-pluginParams"}, description = "Other plugin parameters")
        List<String> pluginParams = new ArrayList<>();
    }

    private final Vaa3dPluginProcessor vaa3dPluginProcessor;

    @Inject
    Vaa3dBlendProcessor(ServiceComputationFactory computationFactory,
                        JacsServiceDataPersistence jacsServiceDataPersistence,
                        @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                        Vaa3dPluginProcessor vaa3dPluginProcessor,
                        Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Vaa3dBlendArgs());
    }

    @Override
    public ServiceResultHandler<File> getResultHandler() {
        return new AbstractSingleFileServiceResultHandler() {

            @Override
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                return getOutputFile(getArgs(jacsServiceData)).toFile().exists();
            }

            @Override
            public File collectResult(JacsServiceData jacsServiceData) {
                return getOutputFile(getArgs(jacsServiceData)).toFile();
            }
        };
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return vaa3dPluginProcessor.getErrorChecker();
    }

    @Override
    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        Vaa3dBlendArgs args = getArgs(jacsServiceData);
        JacsServiceData vaa3dPluginService = createVaa3dPluginService(args, jacsServiceData);
        return vaa3dPluginProcessor.process(vaa3dPluginService)
                .thenApply(r -> jacsServiceData);
    }

    private JacsServiceData createVaa3dPluginService(Vaa3dBlendArgs args, JacsServiceData jacsServiceData) {
        return vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .setServiceName(jacsServiceData.getName())
                        .setErrorPath(jacsServiceData.getErrorPath())
                        .setOutputPath(jacsServiceData.getOutputPath())
                        .state(JacsServiceState.RUNNING).build(),
                new ServiceArg("-plugin", "ifusion.so"),
                new ServiceArg("-pluginFunc", "blender"),
                new ServiceArg("-input", args.inputDir),
                new ServiceArg("-output", args.outputFile),
                new ServiceArg("-pluginParams", String.join(",", args.pluginParams))
        );
    }

    private Vaa3dBlendArgs getArgs(JacsServiceData jacsServiceData) {
        return ServiceArgs.parse(jacsServiceData.getArgsArray(), new Vaa3dBlendArgs());
    }

    private Path getOutputFile(Vaa3dBlendArgs args) {
        return Paths.get(args.outputFile);
    }
}
