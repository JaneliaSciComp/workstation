package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class Vaa3dPluginProcessor extends AbstractServiceProcessor<File> {

    @Inject
    Vaa3dPluginProcessor(JacsServiceEngine jacsServiceEngine,
                         ServiceComputationFactory computationFactory,
                         JacsServiceDataPersistence jacsServiceDataPersistence,
                         @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                         Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
    }

    @Override
    public File getResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToFile(jacsServiceData.getStringifiedResult());
    }

    @Override
    public void setResult(File result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.fileToString(result));
    }

    @Override
    protected ServiceComputation<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        try {
            Vaa3dPluginServiceDescriptor.Vaa3dPluginArgs args = getArgs(jacsServiceData);
            if (StringUtils.isBlank(args.pluginInput)) {
                return computationFactory.newFailedComputation(
                        new ComputationException(jacsServiceData, "Plugin input name must be specified"));
            } else if (StringUtils.isBlank(args.pluginOutput)) {
                return computationFactory.newFailedComputation(
                        new ComputationException(jacsServiceData, "Plugin output name must be specified"));
            } else {
                File pluginOutput = new File(args.pluginOutput);
                try {
                    Files.createDirectories(pluginOutput.getParentFile().toPath());
                } catch (IOException e) {
                    throw new ComputationException(jacsServiceData, e);
                }
                return computationFactory.newCompletedComputation(jacsServiceData);
            }
        } catch (Exception e) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, e));
        }
    }

    @Override
    protected ServiceComputation<File> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        Vaa3dPluginServiceDescriptor.Vaa3dPluginArgs args = getArgs(jacsServiceData);
        return submitVaa3dService(args, jacsServiceData)
                .thenCompose(sd -> this.waitForCompletion(sd))
                .thenCompose(r -> this.collectResult(preProcessingResult, jacsServiceData));
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        Vaa3dPluginServiceDescriptor.Vaa3dPluginArgs args = getArgs(jacsServiceData);
        return Files.exists(Paths.get(args.pluginOutput));
    }

    @Override
    protected File retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        Vaa3dPluginServiceDescriptor.Vaa3dPluginArgs args = getArgs(jacsServiceData);
        return new File(args.pluginOutput);
    }

    private ServiceComputation<JacsServiceData> submitVaa3dService(Vaa3dPluginServiceDescriptor.Vaa3dPluginArgs args, JacsServiceData jacsServiceData) {
        StringJoiner vaa3Args = new StringJoiner(" ")
                .add("-x").add(args.plugin)
                .add("-f").add(args.pluginFunc)
                .add("-i").add(args.pluginInput)
                .add("-o").add(args.pluginOutput);
        if (CollectionUtils.isNotEmpty(args.pluginParams)) {
            vaa3Args.add(StringUtils.wrap(args.pluginParams.stream().collect(Collectors.joining(" ")), '"'));
        }
        JacsServiceData vaa3dCmdService =
                new JacsServiceDataBuilder(jacsServiceData)
                        .setName("vaa3d")
                        .addArg("-vaa3dArgs", vaa3Args.toString())
                        .build();
        return createServiceComputation(jacsServiceEngine.submitSingleService(vaa3dCmdService));
    }

    private Vaa3dPluginServiceDescriptor.Vaa3dPluginArgs getArgs(JacsServiceData jacsServiceData) {
        Vaa3dPluginServiceDescriptor.Vaa3dPluginArgs args = new Vaa3dPluginServiceDescriptor.Vaa3dPluginArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

}
