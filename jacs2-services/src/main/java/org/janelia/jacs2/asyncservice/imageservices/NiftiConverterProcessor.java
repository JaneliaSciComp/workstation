package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractFileListServiceResultHandler;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Named("niftiConverter")
public class NiftiConverterProcessor extends AbstractBasicLifeCycleServiceProcessor<List<File>> {

    static class Vaa3dNiftiConverterArgs extends ServiceArgs {
        @Parameter(names = "-input", description = "Input file", required = true)
        List<String> inputFileNames = new ArrayList<>();
        @Parameter(names = "-output", description = "Output file", required = true)
        List<String> outputFileNames;
        @Parameter(names = {"-p", "-pluginParams"}, description = "Other plugin parameters")
        List<String> pluginParams = new ArrayList<>();
    }

    private final Vaa3dPluginProcessor vaa3dPluginProcessor;

    @Inject
    NiftiConverterProcessor(ServiceComputationFactory computationFactory,
                            JacsServiceDataPersistence jacsServiceDataPersistence,
                            @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                            Vaa3dPluginProcessor vaa3dPluginProcessor,
                            Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Vaa3dNiftiConverterArgs());
    }

    @Override
    public ServiceResultHandler<List<File>> getResultHandler() {
        return new AbstractFileListServiceResultHandler() {
            @Override
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                Vaa3dNiftiConverterArgs args = getArgs(jacsServiceData);
                return args.outputFileNames.stream().reduce(true, (b, fn) -> b && new File(fn).exists(), (b1, b2) -> b1 && b2);
            }

            @Override
            public List<File> collectResult(JacsServiceData jacsServiceData) {
                Vaa3dNiftiConverterArgs args = getArgs(jacsServiceData);
                return args.outputFileNames.stream().map(File::new).filter(File::exists).collect(Collectors.toList());
            }
        };
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return vaa3dPluginProcessor.getErrorChecker();
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        Vaa3dNiftiConverterArgs args = getArgs(jacsServiceData);
        if (CollectionUtils.isEmpty(args.inputFileNames)) {
            throw new ComputationException(jacsServiceData, "An input file name must be specified");
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        Vaa3dNiftiConverterArgs args = getArgs(jacsServiceData);
        JacsServiceData vaa3dPluginService = createVaa3dPluginService(args, jacsServiceData);
        return vaa3dPluginProcessor.process(vaa3dPluginService)
                .thenApply(voidResult -> jacsServiceData);
    }

    private JacsServiceData createVaa3dPluginService(Vaa3dNiftiConverterArgs args, JacsServiceData jacsServiceData) {
        return vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .setServiceName(jacsServiceData.getName())
                        .setErrorPath(jacsServiceData.getErrorPath())
                        .setOutputPath(jacsServiceData.getOutputPath())
                        .state(JacsServiceState.RUNNING).build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "NiftiImageConverter"),
                new ServiceArg("-input", String.join(",", args.inputFileNames)),
                new ServiceArg("-output", String.join(",", args.outputFileNames)),
                new ServiceArg("-pluginParams", String.join(",", args.pluginParams))
        );
    }

    private Vaa3dNiftiConverterArgs getArgs(JacsServiceData jacsServiceData) {
        Vaa3dNiftiConverterArgs args = new Vaa3dNiftiConverterArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }
}
