package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractFileListServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractSingleFileServiceResultHandler;
import org.janelia.jacs2.asyncservice.fileservices.LinkDataProcessor;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentConfiguration;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentUtils;
import org.janelia.jacs2.asyncservice.imageservices.align.ImageCoordinates;
import org.janelia.jacs2.asyncservice.utils.FileUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AlignmentVerificationMovieProcessor generates the alignment verification movie.
 */
@Named("alignRaw")
public class AlignmentVerificationMovieProcessor extends AbstractBasicLifeCycleServiceProcessor<File> {

    static class AlignmentVerificationMoviewArgs extends ServiceArgs {
        @Parameter(names = {"-c", "-config"}, description = "Configuration file", required = true)
        String configFile;
        @Parameter(names = {"-k", "-toolsDir"}, description = "Tools directory", required = true)
        String toolsDir;
        @Parameter(names = {"-s", "-subject"}, description = "Subject file", required = true)
        String subjectFile;
        @Parameter(names = {"-i", "-target"}, description = "Target file", required = true)
        String targetFile;
        @Parameter(names = {"-r", "-reference"}, description = "Reference channel")
        Integer referenceChannel;
        @Parameter(names = {"-o", "-output"}, description = "Output file")
        String outputFile;
    }

    private final Vaa3dConverterProcessor vaa3dConverterProcessor;
    private final Vaa3dPluginProcessor vaa3dPluginProcessor;

    @Inject
    AlignmentVerificationMovieProcessor(ServiceComputationFactory computationFactory,
                                        JacsServiceDataPersistence jacsServiceDataPersistence,
                                        @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                        Vaa3dConverterProcessor vaa3dConverterProcessor,
                                        Vaa3dPluginProcessor vaa3dPluginProcessor,
                                        Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dConverterProcessor = vaa3dConverterProcessor;
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new AlignmentVerificationMoviewArgs());
    }

    @Override
    public ServiceResultHandler<File> getResultHandler() {
        return new AbstractSingleFileServiceResultHandler() {

            @Override
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                AlignmentVerificationMoviewArgs args = getArgs(jacsServiceData);
                File outputFile = new File(args.outputFile);
                return outputFile.exists();
            }

            @Override
            public File collectResult(JacsServiceData jacsServiceData) {
                AlignmentVerificationMoviewArgs args = getArgs(jacsServiceData);
                return new File(args.outputFile);
            }
        };
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            AlignmentVerificationMoviewArgs args = getArgs(jacsServiceData);
            File outputFile = new File(args.outputFile);
            Files.createDirectories(outputFile.getParentFile().toPath());
        } catch (Exception e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        AlignmentVerificationMoviewArgs args = getArgs(jacsServiceData);
        AlignmentConfiguration alignConfig = AlignmentUtils.parseAlignConfig(args.configFile);

        JacsServiceData jacsServiceDataHierarchy = jacsServiceDataPersistence.findServiceHierarchy(jacsServiceData.getId());

        return ImmutableList.of();
    }

    @Override
    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(jacsServiceData);
    }

    private AlignmentVerificationMoviewArgs getArgs(JacsServiceData jacsServiceData) {
        return AlignmentVerificationMoviewArgs.parse(jacsServiceData.getArgsArray(), new AlignmentVerificationMoviewArgs());
    }

}
