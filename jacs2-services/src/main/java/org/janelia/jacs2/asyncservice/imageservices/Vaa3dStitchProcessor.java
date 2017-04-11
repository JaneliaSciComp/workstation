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
import org.janelia.jacs2.asyncservice.common.resulthandlers.VoidServiceResultHandler;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

@Named("vaa3dStitch")
public class Vaa3dStitchProcessor extends AbstractBasicLifeCycleServiceProcessor<Void> {

    static class Vaa3dStitchArgs extends ServiceArgs {
        @Parameter(names = "-inputDir", description = "Input directory", required = true)
        String inputDir;
        @Parameter(names = "-refchannel", description = "Reference channel")
        int referenceChannel = 4;
        @Parameter(names = {"-p", "-pluginParams"}, description = "Other plugin parameters")
        List<String> pluginParams = new ArrayList<>();
    }

    private final Vaa3dPluginProcessor vaa3dPluginProcessor;

    @Inject
    Vaa3dStitchProcessor(ServiceComputationFactory computationFactory,
                         JacsServiceDataPersistence jacsServiceDataPersistence,
                         @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                         Vaa3dPluginProcessor vaa3dPluginProcessor,
                         Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Vaa3dStitchArgs());
    }

    @Override
    public ServiceResultHandler<Void> getResultHandler() {
        return new VoidServiceResultHandler();
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return vaa3dPluginProcessor.getErrorChecker();
    }

    @Override
    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        Vaa3dStitchArgs args = getArgs(jacsServiceData);
        JacsServiceData vaa3dPluginService = createVaa3dPluginService(args, jacsServiceData);
        return vaa3dPluginProcessor.process(vaa3dPluginService)
                .thenApply(r -> jacsServiceData);
    }

    private JacsServiceData createVaa3dPluginService(Vaa3dStitchArgs args, JacsServiceData jacsServiceData) {
        return vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .setServiceName(jacsServiceData.getName())
                        .setErrorPath(jacsServiceData.getErrorPath())
                        .setOutputPath(jacsServiceData.getOutputPath())
                        .state(JacsServiceState.RUNNING).build(),
                new ServiceArg("-plugin", "imageStitch.so"),
                new ServiceArg("-pluginFunc", "v3dstitch"),
                new ServiceArg("-input", args.inputDir),
                new ServiceArg("-pluginParams", String.format("#c %d", args.referenceChannel)),
                new ServiceArg("-pluginParams", String.join(",", args.pluginParams))
        );
    }

    private Vaa3dStitchArgs getArgs(JacsServiceData jacsServiceData) {
        Vaa3dStitchArgs args = new Vaa3dStitchArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

}
