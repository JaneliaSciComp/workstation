package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.DefaultServiceErrorChecker;
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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Named("vaa3dPlugin")
public class Vaa3dPluginProcessor extends AbstractBasicLifeCycleServiceProcessor<List<File>> {

    static class Vaa3dPluginArgs extends ServiceArgs {
        @Parameter(names = {"-x", "-plugin"}, description = "Vaa3d plugin name", required = true)
        String plugin;
        @Parameter(names = {"-f", "-pluginFunc"}, description = "Vaa3d plugin function", required = true)
        String pluginFunc;
        @Parameter(names = {"-i", "-input"}, description = "Plugin input", required = false)
        List<String> pluginInputs = new ArrayList<>();
        @Parameter(names = {"-o", "-output"}, description = "Plugin output", required = false)
        List<String> pluginOutputs = new ArrayList<>();
        @Parameter(names = {"-p", "-pluginParams"}, description = "Plugin parameters")
        List<String> pluginParams = new ArrayList<>();
    }

    private final Vaa3dProcessor vaa3dProcessor;

    @Inject
    Vaa3dPluginProcessor(ServiceComputationFactory computationFactory,
                         JacsServiceDataPersistence jacsServiceDataPersistence,
                         @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                         Vaa3dProcessor vaa3dProcessor,
                         Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dProcessor = vaa3dProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Vaa3dPluginArgs());
    }

    @Override
    public ServiceResultHandler<List<File>> getResultHandler() {
        return new AbstractFileListServiceResultHandler() {
            @Override
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                Vaa3dPluginArgs args = getArgs(jacsServiceData);
                return args.pluginOutputs.stream().reduce(true, (b, fn) -> b && new File(fn).exists(), (b1, b2) -> b1 && b2);
            }

            @Override
            public List<File> collectResult(JacsServiceData jacsServiceData) {
                Vaa3dPluginArgs args = getArgs(jacsServiceData);
                return args.pluginOutputs.stream().map(File::new).filter(File::exists).collect(Collectors.toList());
            }
        };
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return new DefaultServiceErrorChecker(logger) {
            @Override
            protected boolean hasErrors(String l) {
                boolean result = super.hasErrors(l);
                if (result) {
                    return true;
                }
                if (StringUtils.isNotBlank(l) && l.matches("(?i:.*(fail to call the plugin).*)")) {
                    logger.error(l);
                    return true;
                } else {
                    return false;
                }
            }
        };
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            Vaa3dPluginArgs args = getArgs(jacsServiceData);
            args.pluginOutputs.forEach(o -> {
                File oFile = new File(o);
                try {
                    Files.createDirectories(oFile.getParentFile().toPath());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (Exception e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return jacsServiceData;
    }

    @Override
    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        Vaa3dPluginArgs args = getArgs(jacsServiceData);
        JacsServiceData vaa3dService = createVaa3dService(args, jacsServiceData);
        return vaa3dProcessor.process(vaa3dService)
                .thenApply(voidResult -> {
                    jacsServiceData.setOutputPath(vaa3dService.getOutputPath());
                    jacsServiceData.setErrorPath(vaa3dService.getErrorPath());
                    return jacsServiceData;
                });
    }

    private JacsServiceData createVaa3dService(Vaa3dPluginArgs args, JacsServiceData jacsServiceData) {
        StringJoiner vaa3Args = new StringJoiner(" ")
                .add("-x").add(args.plugin)
                .add("-f").add(args.pluginFunc);
        if (CollectionUtils.isNotEmpty(args.pluginInputs)) {
            vaa3Args.add("-i").add(args.pluginInputs.stream().collect(Collectors.joining(" ")));
        }
        if (CollectionUtils.isNotEmpty(args.pluginOutputs)) {
            vaa3Args.add("-o").add(args.pluginOutputs.stream().collect(Collectors.joining(" ")));
        }
        if (CollectionUtils.isNotEmpty(args.pluginParams)) {
            vaa3Args.add("-p").add(StringUtils.wrap(args.pluginParams.stream().collect(Collectors.joining(" ")), '"'));
        }
        return vaa3dProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(JacsServiceState.RUNNING).build(),
                new ServiceArg("-vaa3dArgs", vaa3Args.toString())
        );
    }

    private Vaa3dPluginArgs getArgs(JacsServiceData jacsServiceData) {
        Vaa3dPluginArgs args = new Vaa3dPluginArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

}
