package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
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
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringJoiner;

@Named("distortionCorrection")
public class DistortionCorrectionProcessor extends AbstractBasicLifeCycleServiceProcessor<Void, File> {

    static class DistortionCorrectionArgs extends ServiceArgs {
        @Parameter(names = "-inputFile", description = "Input file", required = true)
        String inputFile;
        @Parameter(names = "-outputFile", description = "Output file", required = true)
        String outputFile;
        @Parameter(names = "-microscope", description = "Microscope name", required = true)
        String microscope;
    }

    private final String distortionCorrectionMacro;
    private final FijiMacroProcessor fijiMacroProcessor;

    @Inject
    DistortionCorrectionProcessor(ServiceComputationFactory computationFactory,
                                  JacsServiceDataPersistence jacsServiceDataPersistence,
                                  @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                  @PropertyValue(name = "Fiji.DistortionCorrection") String distortionCorrectionMacro,
                                  FijiMacroProcessor fijiMacroProcessor,
                                  Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.distortionCorrectionMacro = distortionCorrectionMacro;
        this.fijiMacroProcessor = fijiMacroProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new DistortionCorrectionArgs());
    }

    @Override
    public ServiceResultHandler<File> getResultHandler() {
        return new AbstractSingleFileServiceResultHandler() {
            @Override
            public boolean isResultReady(JacsServiceResult<?> depResults) {
                return getOutputFile(getArgs(depResults.getJacsServiceData())).toFile().exists();
            }

            @Override
            public File collectResult(JacsServiceResult<?> depResults) {
                return getOutputFile(getArgs(depResults.getJacsServiceData())).toFile();
            }
        };
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return fijiMacroProcessor.getErrorChecker();
    }

    @Override
    protected ServiceComputation<JacsServiceResult<Void>> processing(JacsServiceResult<Void> depResults) {
        DistortionCorrectionArgs args = getArgs(depResults.getJacsServiceData());
        JacsServiceData fijiService = createFijiService(args, depResults.getJacsServiceData());
        return fijiMacroProcessor.process(fijiService)
                .thenApply(voidResult -> depResults);
    }

    private JacsServiceData createFijiService(DistortionCorrectionArgs args, JacsServiceData jacsServiceData) {
        return fijiMacroProcessor.createServiceData(new ServiceExecutionContext(jacsServiceData),
                new ServiceArg("-macro", distortionCorrectionMacro),
                new ServiceArg("-macroArgs", getMacroArgs(args)),
                new ServiceArg("-finalOutput", getOutputDir(args).toString()),
                new ServiceArg("-resultsPatterns", new File(args.outputFile).getName())
        );
    }

    private String getMacroArgs(DistortionCorrectionArgs args) {
        StringJoiner builder = new StringJoiner(",");
        builder.add(String.format("%s/", getInputDir(args)));
        builder.add(String.format("%s", getInputFile(args).getFileName()));
        builder.add(String.format("%s/", getOutputDir(args)));
        builder.add(StringUtils.wrap(args.microscope, '"'));
        return builder.toString();
    }

    private DistortionCorrectionArgs getArgs(JacsServiceData jacsServiceData) {
        return ServiceArgs.parse(jacsServiceData.getArgsArray(), new DistortionCorrectionArgs());
    }

    private Path getInputFile(DistortionCorrectionArgs args) {
        return Paths.get(args.inputFile).toAbsolutePath();
    }

    private Path getInputDir(DistortionCorrectionArgs args) {
        return getInputFile(args).getParent();
    }

    private Path getOutputFile(DistortionCorrectionArgs args) {
        return Paths.get(args.outputFile);
    }

    private Path getOutputDir(DistortionCorrectionArgs args) {
        return getOutputFile(args).getParent();
    }
}
