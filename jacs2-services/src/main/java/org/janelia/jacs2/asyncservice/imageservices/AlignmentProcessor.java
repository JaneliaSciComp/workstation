package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractFileListServiceResultHandler;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentConfiguration;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentInput;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentUtils;
import org.janelia.jacs2.asyncservice.utils.FileUtils;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Named("align")
public class AlignmentProcessor extends AbstractBasicLifeCycleServiceProcessor<List<JacsServiceData>, List<File>> {

    static class AlignmentArgs extends ServiceArgs {
        @Parameter(names = {"-nthreads"}, description = "Number of ITK threads")
        Integer nthreads = 16;
        @Parameter(names = {"-i", "-input1"}, description = "The alignment first input", required = true)
        String input1;
        @Parameter(names = {"-e", "-i1Neurons"}, description = "Input1 neurons file", required = false)
        String input1Neurons;
        @Parameter(names = {"-j", "-input2"}, description = "The alignment second input", required = false)
        String input2;
        @Parameter(names = {"-f", "-i2Neurons"}, description = "Input2 neurons file", required = false)
        String input2Neurons;
        @Parameter(names = {"-c", "-config"}, description = "Configuration file", required = true)
        String configFile;
        @Parameter(names = {"-t", "-templateDir"}, description = "Template directory", required = true)
        String templateDir;
        @Parameter(names = {"-k", "-toolsDir"}, description = "Tools directory", required = true)
        String toolsDir;
        @Parameter(names = {"-s", "-step"}, description = "Step", required = false)
        String step;
        @Parameter(names = {"-m", "-mp", "-mountingProtocol"}, description = "Mounting protocol", required = false)
        String mountingProtocol;
        @Parameter(names = {"-g", "-gender"}, description = "Gender", required = false)
        String gender;
        @Parameter(names = {"-z", "-zflip"}, arity = 0, description = "Z flip flag", required = false)
        boolean zFlip = false;
        @Parameter(names = "-fslOutputType", description = "FSL output type", required = false)
        String fslOutputType = "NIFTI_GZ";
        @Parameter(names = {"-o", "-w", "-resultsDir"}, description = "Results directory", required = false)
        String resultsDir;
    }

    private final RawFilesAlignmentProcessor rawFilesAlignmentProcessor;

    @Inject
    AlignmentProcessor(ServiceComputationFactory computationFactory,
                       JacsServiceDataPersistence jacsServiceDataPersistence,
                       @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                       RawFilesAlignmentProcessor rawFilesAlignmentProcessor,
                       Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.rawFilesAlignmentProcessor = rawFilesAlignmentProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new AlignmentArgs());
    }

    @Override
    public ServiceResultHandler<List<File>> getResultHandler() {
        return new AbstractFileListServiceResultHandler() {
            final String resultsPattern = "glob:**/*.{v3draw}";

            @Override
            public boolean isResultReady(JacsServiceResult<?> depResults) {
                return areAllDependenciesDone(depResults.getJacsServiceData());
            }

            @Override
            public List<File> collectResult(JacsServiceResult<?> depResults) {
                AlignmentArgs args = getArgs(depResults.getJacsServiceData());
                return FileUtils.lookupFiles(getResultsDir(args), 1, resultsPattern)
                        .map(Path::toFile)
                        .collect(Collectors.toList());
            }
        };
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            AlignmentArgs args = getArgs(jacsServiceData);
            Files.createDirectories(getResultsDir(args));
        } catch (IOException e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected JacsServiceResult<List<JacsServiceData>> submitServiceDependencies(JacsServiceData jacsServiceData) {
        AlignmentArgs args = getArgs(jacsServiceData);
        AlignmentConfiguration alignConfig = AlignmentUtils.parseAlignConfig(args.configFile);
        AlignmentInput input1 = AlignmentUtils.parseInput(args.input1);
        AlignmentInput input2 = AlignmentUtils.parseInput(args.input2);

        List<ServiceArg> alignmentArgs = new LinkedList<>();
        if (args.nthreads > 0) alignmentArgs.add(new ServiceArg("-nthreads", String.valueOf(args.nthreads)));
        if (StringUtils.isNotBlank(input1.name)) alignmentArgs.add(new ServiceArg("-i1File", input1.name));
        if (StringUtils.isNotBlank(input1.channels)) alignmentArgs.add(new ServiceArg("-i1Channels", input1.channels));
        if (StringUtils.isNotBlank(input1.ref)) alignmentArgs.add(new ServiceArg("-i1Ref", input1.ref));
        if (StringUtils.isNotBlank(input1.res)) alignmentArgs.add(new ServiceArg("-i1Res", input1.res));
        if (StringUtils.isNotBlank(input1.dims)) alignmentArgs.add(new ServiceArg("-i1Dims", input1.dims));
        if (StringUtils.isNotBlank(args.input1Neurons)) alignmentArgs.add(new ServiceArg("-i1Neurons", args.input1Neurons));

        if (StringUtils.isNotBlank(input2.name)) alignmentArgs.add(new ServiceArg("-i2File", input2.name));
        if (StringUtils.isNotBlank(input2.channels)) alignmentArgs.add(new ServiceArg("-i1Channels", input2.channels));
        if (StringUtils.isNotBlank(input2.ref)) alignmentArgs.add(new ServiceArg("-i2Ref", input2.ref));
        if (StringUtils.isNotBlank(input2.res)) alignmentArgs.add(new ServiceArg("-i2Res", input2.res));
        if (StringUtils.isNotBlank(input2.dims)) alignmentArgs.add(new ServiceArg("-i2Dims", input2.dims));
        if (StringUtils.isNotBlank(args.input2Neurons)) alignmentArgs.add(new ServiceArg("-i1Neurons", args.input2Neurons));

        alignmentArgs.add(new ServiceArg("-config", args.configFile));
        alignmentArgs.add(new ServiceArg("-templateDir", args.templateDir));
        alignmentArgs.add(new ServiceArg("-toolsDir", args.toolsDir));
        alignmentArgs.add(new ServiceArg("-step", args.step));
        alignmentArgs.add(new ServiceArg("-mountingProtocol", args.mountingProtocol));


        if (args.zFlip) alignmentArgs.add(new ServiceArg("-zflip"));
        if (StringUtils.isNotBlank(args.fslOutputType)) alignmentArgs.add(new ServiceArg("-fslOutputType", args.fslOutputType));
        alignmentArgs.add(new ServiceArg("-resultsDir", args.resultsDir));

        if ("m".equals(args.gender)) {
            // male must be explicit
            alignmentArgs.add(new ServiceArg("-targetTemplate", alignConfig.templates.mfbSxDpx));
            alignmentArgs.add(new ServiceArg("-targetExtTemplate", alignConfig.templates.mfbSxDpxExt));
            alignmentArgs.add(new ServiceArg("-alignmentSpace", "Barry 63x Subsampled Alignment Space"));
        } else {
            // otherwise select female templates
            alignmentArgs.add(new ServiceArg("-targetTemplate", alignConfig.templates.cbmCfo));
            alignmentArgs.add(new ServiceArg("-targetExtTemplate", alignConfig.templates.cbmCfoExt));
            alignmentArgs.add(new ServiceArg("-alignmentSpace", "Yoshi 63x Subsampled Alignment Space"));
        }

        JacsServiceData jacsServiceDataHierarchy = jacsServiceDataPersistence.findServiceHierarchy(jacsServiceData.getId());

        JacsServiceData alignServiceDataRef = rawFilesAlignmentProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(JacsServiceState.QUEUED)
                        .build(),
                alignmentArgs.toArray(new ServiceArg[alignmentArgs.size()]));

        JacsServiceData alignServiceData = submitDependencyIfNotPresent(jacsServiceDataHierarchy, alignServiceDataRef);

        return new JacsServiceResult<>(jacsServiceData, ImmutableList.of(alignServiceData));
    }

    @Override
    protected ServiceComputation<JacsServiceResult<List<JacsServiceData>>> processing(JacsServiceResult<List<JacsServiceData>> depResults) {
        return computationFactory.newCompletedComputation(depResults);
    }

    private Path getResultsDir(AlignmentArgs args) {
        return Paths.get(args.resultsDir);
    }

    private AlignmentArgs getArgs(JacsServiceData jacsServiceData) {
        return AlignmentArgs.parse(jacsServiceData.getArgsArray(), new AlignmentArgs());
    }
}
