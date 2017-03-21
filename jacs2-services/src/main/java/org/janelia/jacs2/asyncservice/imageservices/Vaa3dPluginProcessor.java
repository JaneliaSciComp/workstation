package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceCommand;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
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
public class Vaa3dPluginProcessor extends AbstractBasicLifeCycleServiceProcessor<List<File>> implements ServiceCommand {

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
    Vaa3dPluginProcessor(JacsServiceEngine jacsServiceEngine,
                         ServiceComputationFactory computationFactory,
                         JacsServiceDataPersistence jacsServiceDataPersistence,
                         @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                         Logger logger,
                         Vaa3dProcessor vaa3dProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dProcessor = vaa3dProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Vaa3dPluginArgs());
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
        try {
            Vaa3dPluginArgs args = getArgs(jacsServiceData);
            try {
                args.pluginOutputs.forEach(o -> {
                    File oFile = new File(o);
                    try {
                        Files.createDirectories(oFile.getParentFile().toPath());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (Exception e) {
                return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, e));
            }
            return createComputation(jacsServiceData);
        } catch (Exception e) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, e));
        }
    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        Vaa3dPluginArgs args = getArgs(jacsServiceData);
        return ImmutableList.of(submitVaa3dService(args, jacsServiceData, jacsServiceData.getState()));
    }

    @Override
    protected ServiceComputation<List<File>> processing(JacsServiceData jacsServiceData) {
        return createComputation(this.waitForResult(jacsServiceData));
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        return checkForDependenciesCompletion(jacsServiceData);
    }

    @Override
    protected List<File> retrieveResult(JacsServiceData jacsServiceData) {
        Vaa3dPluginArgs args = getArgs(jacsServiceData);
        List<File> results = new ArrayList<>();
        args.pluginOutputs.forEach(o -> {
            File oFile = new File(o);
            if (oFile.exists()) {
                results.add(oFile);
            }
        });
        return results;
    }

    private JacsServiceData submitVaa3dService(Vaa3dPluginArgs args, JacsServiceData jacsServiceData, JacsServiceState vaa3dServiceState) {
        StringJoiner vaa3Args = new StringJoiner(" ")
                .add("-x").add(args.plugin)
                .add("-f").add(args.pluginFunc);
        if (CollectionUtils.isNotEmpty(args.pluginInputs)) {
            vaa3Args.add("-i").add(args.pluginParams.stream().collect(Collectors.joining(" ")));
        }
        if (CollectionUtils.isNotEmpty(args.pluginOutputs)) {
            vaa3Args.add("-o").add(args.pluginOutputs.stream().collect(Collectors.joining(" ")));
        }
        if (CollectionUtils.isNotEmpty(args.pluginParams)) {
            vaa3Args.add("-p").add(StringUtils.wrap(args.pluginParams.stream().collect(Collectors.joining(" ")), '"'));
        }
        return submit(vaa3dProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(vaa3dServiceState).build(),
                new ServiceArg("-vaa3dArgs", vaa3Args.toString())));
    }

    @Override
    public void execute(JacsServiceData jacsServiceData) {
        execute(sd -> {
            Vaa3dPluginArgs args = getArgs(sd);
            vaa3dProcessor.execute(submitVaa3dService(args, sd, JacsServiceState.RUNNING));
        }, jacsServiceData);
    }

    private Vaa3dPluginArgs getArgs(JacsServiceData jacsServiceData) {
        Vaa3dPluginArgs args = new Vaa3dPluginArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

}
