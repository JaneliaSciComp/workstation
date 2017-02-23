package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
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

public class Vaa3dConverterProcessor extends AbstractServiceProcessor<File> {

    @Inject
    Vaa3dConverterProcessor(JacsServiceEngine jacsServiceEngine,
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
            Vaa3dConverterServiceDescriptor.Vaa3dConverterArgs args = getArgs(jacsServiceData);
            if (StringUtils.isBlank(args.inputFileName)) {
                return computationFactory.newFailedComputation(
                        new ComputationException(jacsServiceData, "Input file name must be specified"));
            } else if (StringUtils.isBlank(args.outputFileName)) {
                return computationFactory.newFailedComputation(
                        new ComputationException(jacsServiceData, "Output file name must be specified"));
            } else {
                File outputFile = new File(args.outputFileName);
                try {
                    Files.createDirectories(outputFile.getParentFile().toPath());
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
        Vaa3dConverterServiceDescriptor.Vaa3dConverterArgs args = getArgs(jacsServiceData);
        return submitVaa3dCmdService(args, jacsServiceData)
                .thenCompose(sd -> this.waitForCompletion(sd))
                .thenCompose(r -> this.collectResult(preProcessingResult, jacsServiceData));
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        Vaa3dConverterServiceDescriptor.Vaa3dConverterArgs args = getArgs(jacsServiceData);
        return Files.exists(Paths.get(args.outputFileName));
    }

    @Override
    protected File retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        Vaa3dConverterServiceDescriptor.Vaa3dConverterArgs args = getArgs(jacsServiceData);
        return new File(args.outputFileName);
    }

    private ServiceComputation<JacsServiceData> submitVaa3dCmdService(Vaa3dConverterServiceDescriptor.Vaa3dConverterArgs args, JacsServiceData jacsServiceData) {
        StringJoiner vaa3dCmdArgs = new StringJoiner(" ")
                .add(args.convertCmd)
                .add(args.inputFileName)
                .add(args.outputFileName);
        JacsServiceData vaa3dCmdService =
                new JacsServiceDataBuilder(jacsServiceData)
                        .setName("vaa3dCmd")
                        .addArg("-vaa3dCmd", "image-loader")
                        .addArg("-vaa3dCmdArgs", vaa3dCmdArgs.toString())
                        .build();
        return createServiceComputation(jacsServiceEngine.submitSingleService(vaa3dCmdService));
    }

    private Vaa3dConverterServiceDescriptor.Vaa3dConverterArgs getArgs(JacsServiceData jacsServiceData) {
        Vaa3dConverterServiceDescriptor.Vaa3dConverterArgs args = new Vaa3dConverterServiceDescriptor.Vaa3dConverterArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

}
