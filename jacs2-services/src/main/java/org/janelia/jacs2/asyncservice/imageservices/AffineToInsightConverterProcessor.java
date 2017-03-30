package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractSingleFileServiceResultHandler;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentUtils;
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

@Named("afine2InsightConverter")
public class AffineToInsightConverterProcessor extends AbstractBasicLifeCycleServiceProcessor<File> {

    static class AfineToInsightConverterArgs extends ServiceArgs {
        @Parameter(names = "-input", description = "Input file", required = true)
        String input;
        @Parameter(names = "-output", description = "Output file", required = true)
        String output;
    }

    @Inject
    AffineToInsightConverterProcessor(ServiceComputationFactory computationFactory,
                                      JacsServiceDataPersistence jacsServiceDataPersistence,
                                      @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                      Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new AfineToInsightConverterArgs());
    }

    @Override
    public ServiceResultHandler<File> getResultHandler() {
        return new AbstractSingleFileServiceResultHandler() {

            @Override
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                AfineToInsightConverterArgs args = getArgs(jacsServiceData);
                return getOutput(args).toFile().exists();
            }

            @Override
            public File collectResult(JacsServiceData jacsServiceData) {
                AfineToInsightConverterArgs args = getArgs(jacsServiceData);
                return getOutput(args).toFile();
            }
        };
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        AfineToInsightConverterArgs args = getArgs(jacsServiceData);
        if (!getInput(args).toFile().exists()) {
            throw new ComputationException(jacsServiceData, "Affine input file " + args.input + " not found");
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        AfineToInsightConverterArgs args = getArgs(jacsServiceData);
        AlignmentUtils.convertAffineMatToInsightMat(getInput(args), getOutput(args));
        return computationFactory.newCompletedComputation(jacsServiceData);
    }

    private AfineToInsightConverterArgs getArgs(JacsServiceData jacsServiceData) {
        AfineToInsightConverterArgs args = new AfineToInsightConverterArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private Path getInput(AfineToInsightConverterArgs args) {
        return Paths.get(args.input);
    }

    private Path getOutput(AfineToInsightConverterArgs args) {
        return Paths.get(args.output);
    }
}
