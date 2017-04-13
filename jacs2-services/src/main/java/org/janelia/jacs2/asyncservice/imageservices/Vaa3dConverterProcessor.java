package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
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
import java.nio.file.Files;
import java.util.StringJoiner;

@Named("vaa3dConverter")
public class Vaa3dConverterProcessor extends AbstractBasicLifeCycleServiceProcessor<Void, File> {

    static class Vaa3dConverterArgs extends ServiceArgs {
        @Parameter(names = "-convertCmd", description = "Convert command. Valid values are: []")
        String convertCmd = "-convert";
        @Parameter(names = "-input", description = "Input file", required = true)
        String inputFileName;
        @Parameter(names = "-output", description = "Output file", required = true)
        String outputFileName;
    }

    private final Vaa3dCmdProcessor vaa3dCmdProcessor;

    @Inject
    Vaa3dConverterProcessor(ServiceComputationFactory computationFactory,
                            JacsServiceDataPersistence jacsServiceDataPersistence,
                            @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                            Vaa3dCmdProcessor vaa3dCmdProcessor,
                            Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dCmdProcessor = vaa3dCmdProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Vaa3dConverterArgs());
    }

    @Override
    public ServiceResultHandler<File> getResultHandler() {
        return new AbstractSingleFileServiceResultHandler() {

            @Override
            public boolean isResultReady(JacsServiceResult<?> depResults) {
                Vaa3dConverterArgs args = getArgs(depResults.getJacsServiceData());
                File outputFile = new File(args.outputFileName);
                return outputFile.exists();
            }

            @Override
            public File collectResult(JacsServiceResult<?> depResults) {
                Vaa3dConverterArgs args = getArgs(depResults.getJacsServiceData());
                return new File(args.outputFileName);
            }
        };
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return vaa3dCmdProcessor.getErrorChecker();
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            Vaa3dConverterArgs args = getArgs(jacsServiceData);
            if (StringUtils.isBlank(args.inputFileName)) {
                throw new ComputationException(jacsServiceData, "Input file name must be specified");
            } else if (StringUtils.isBlank(args.outputFileName)) {
                throw new ComputationException(jacsServiceData, "Output file name must be specified");
            } else {
                File outputFile = new File(args.outputFileName);
                Files.createDirectories(outputFile.getParentFile().toPath());
            }
        } catch (ComputationException e) {
            throw e;
        } catch (Exception e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected ServiceComputation<JacsServiceResult<Void>> processing(JacsServiceResult<Void> depResults) {
        Vaa3dConverterArgs args = getArgs(depResults.getJacsServiceData());
        JacsServiceData vaa3dService = createVaa3dCmdService(args, depResults.getJacsServiceData());
        return vaa3dCmdProcessor.process(vaa3dService)
                .thenApply(voidResult -> depResults);
    }

    private JacsServiceData createVaa3dCmdService(Vaa3dConverterArgs args, JacsServiceData jacsServiceData) {
        StringJoiner vaa3dCmdArgs = new StringJoiner(" ")
                .add(args.convertCmd)
                .add(args.inputFileName)
                .add(args.outputFileName);
        return vaa3dCmdProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .setServiceName(jacsServiceData.getName())
                        .setErrorPath(jacsServiceData.getErrorPath())
                        .setOutputPath(jacsServiceData.getOutputPath())
                        .state(JacsServiceState.RUNNING).build(),
                new ServiceArg("-vaa3dCmd", "image-loader"),
                new ServiceArg("-vaa3dCmdArgs", vaa3dCmdArgs.toString()));
    }

    private Vaa3dConverterArgs getArgs(JacsServiceData jacsServiceData) {
        return ServiceArgs.parse(jacsServiceData.getArgsArray(), new Vaa3dConverterArgs());
    }

}
