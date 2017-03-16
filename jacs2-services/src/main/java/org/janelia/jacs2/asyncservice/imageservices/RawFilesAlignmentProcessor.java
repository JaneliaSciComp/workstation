package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.IterableUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentConfiguration;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentUtils;
import org.janelia.jacs2.asyncservice.imageservices.align.ImageCoordinates;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * RawFilesAlignmentProcessor executes the alignment "pipeline" and assumes all files are already
 * in v3draw format.
 */
@Named("alignRaw")
public class RawFilesAlignmentProcessor extends AbstractBasicLifeCycleServiceProcessor<List<File>> {

    static class AlignmentArgs extends ServiceArgs {
        @Parameter(names = {"-nthreads"}, description = "Number of ITK threads")
        Integer nthreads = 16;
        @Parameter(names = "-i1File", description = "The name of the first input file", required = true)
        String input1File;
        @Parameter(names = "-i1Channels", description = "The channels of the first input file", required = true)
        String input1Channels;
        @Parameter(names = "-i1Ref", description = "The reference for the first input file", required = true)
        String input1Ref;
        @Parameter(names = "-i1Res", description = "The resolution of the first input file", required = true)
        String input1Res;
        @Parameter(names = "-i1Dims", description = "The dimensions of the first input file", required = false)
        String input1Dims;
        @Parameter(names = {"-e", "-i1Neurons"}, description = "Input1 neurons file", required = false)
        String input1Neurons;
        @Parameter(names = "-i2File", description = "The name of the second input file", required = false)
        String input2File;
        @Parameter(names = "-i2Channels", description = "The channels of the second input file", required = false)
        String input2Channels;
        @Parameter(names = "-i2Ref", description = "The reference for the second input file", required = false)
        String input2Ref;
        @Parameter(names = "-i2Res", description = "The resolution of the second input file", required = false)
        String input2Res;
        @Parameter(names = "-i2Dims", description = "The dimensions of the second input file", required = false)
        String input2Dims;
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
        @Parameter(names = {"-targetTemplate"}, description = "Target template", required = false)
        String targetTemplate;
        @Parameter(names = {"-targetExtTemplate"}, description = "Target EXT template", required = false)
        String targetExtTemplate;
        @Parameter(names = {"-z", "-zflip"}, arity = 0, description = "Z flip flag", required = false)
        boolean zFlip = false;
        @Parameter(names = "-fslOutputType", description = "FSL output type", required = false)
        String fslOutputType = "NIFTI_GZ";
        @Parameter(names = {"-o", "-w", "-resultsDir"}, description = "Results directory", required = false)
        String resultsDir;
    }

    private final Vaa3dConverterProcessor vaa3dConverterProcessor;
    private final Vaa3dPluginProcessor vaa3dPluginProcessor;
    private final NiftiConverterProcessor niftiConverterProcessor;

    @Inject
    RawFilesAlignmentProcessor(JacsServiceEngine jacsServiceEngine,
                               ServiceComputationFactory computationFactory,
                               JacsServiceDataPersistence jacsServiceDataPersistence,
                               @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                               @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                               @Any Instance<ExternalProcessRunner> serviceRunners,
                               Logger logger,
                               Vaa3dConverterProcessor vaa3dConverterProcessor,
                               Vaa3dPluginProcessor vaa3dPluginProcessor,
                               NiftiConverterProcessor niftiConverterProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dConverterProcessor = vaa3dConverterProcessor;
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
        this.niftiConverterProcessor = niftiConverterProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new AlignmentArgs());
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
        AlignmentArgs args = getArgs(jacsServiceData);
        try {
            Files.createDirectories(getResultsDir(args));
        } catch (IOException e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return createComputation(jacsServiceData);
    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        AlignmentArgs args = getArgs(jacsServiceData);
        convertNeuronsFileToRawFormat(args.input1Neurons, jacsServiceData);
        convertTargetExtToNiftiImage(args, jacsServiceData);
        zFlipSubject(args, jacsServiceData);
        isotropicSubjectSampling(args, jacsServiceData);
        JacsServiceData resizeSubject = resizeSubjectToTarget(args, jacsServiceData);
        extractRefFromSubject(args, jacsServiceData, resizeSubject);
        return ImmutableList.of(); // FIXME!!!!
    }

    private JacsServiceData convertNeuronsFileToRawFormat(String neuronsFile, JacsServiceData jacsServiceData) {
        if ("v3draw".equals(com.google.common.io.Files.getFileExtension(neuronsFile))) {
            try {
                Files.createLink(getWorkingNeuronsFile(neuronsFile, jacsServiceData), Paths.get(neuronsFile));
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return vaa3dConverterProcessor.submit(new ServiceExecutionContext.Builder(jacsServiceData)
                            .state(JacsServiceState.RUNNING)
                            .build(),
                    new ServiceArg("-input", neuronsFile),
                    new ServiceArg("-output", getWorkingNeuronsFile(neuronsFile, jacsServiceData).toString()));
        }
    }

    private JacsServiceData convertTargetExtToNiftiImage(AlignmentArgs args, JacsServiceData jacsServiceData) {
        Path targetExtFile = getTargetExtFile(args);
        logger.info("Convert {} into a nifti image", targetExtFile);
        JacsServiceData niftiConverterServiceData = niftiConverterProcessor.submit(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-input", targetExtFile.toString()),
                new ServiceArg("-output", getNiftiTargetExtFile(args, jacsServiceData).toString()));

        niftiConverterProcessor.execute(niftiConverterServiceData);
        return niftiConverterServiceData;
    }

    private void zFlipSubject(AlignmentArgs args, JacsServiceData jacsServiceData) {
        if (args.zFlip) {
            logger.info("Flip {} along the z axis", args.input1File);
            JacsServiceData zFlipSubjectsServiceData = vaa3dPluginProcessor.submit(new ServiceExecutionContext.Builder(jacsServiceData)
                            .state(JacsServiceState.RUNNING)
                            .build(),
                    new ServiceArg("-plugin", "ireg"),
                    new ServiceArg("-pluginFunc", "zflip"),
                    new ServiceArg("-input", args.input1File),
                    new ServiceArg("-output", getWorkingSubjectFile(args, jacsServiceData).toString()));
            vaa3dPluginProcessor.execute(zFlipSubjectsServiceData);
        } else {
            try {
                Files.createLink(getWorkingSubjectFile(args, jacsServiceData), Paths.get(args.input1File));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void isotropicSubjectSampling(AlignmentArgs args, JacsServiceData jacsServiceData) {
        Path workingSubjectFile = getWorkingSubjectFile(args, jacsServiceData);
        Path isotropicSubjectFile = getWorkingIsotropicSubjectFile(args, jacsServiceData);
        AlignmentConfiguration alignConfig = AlignmentUtils.parseAlignConfig(args.configFile);
        ImageCoordinates res = AlignmentUtils.parseCoordinates(args.input1Res);
        double isx = res.x / alignConfig.misc.vSzIsX63x;
        double isy = res.y / alignConfig.misc.vSzIsY63x;
        double isz = res.z / alignConfig.misc.vSzIsZ63x;
        if (Math.abs(isx - 1.) >  0.01
                || Math.abs(isy - 1.) >  0.01
                || Math.abs(isz - 1.) >  0.01) {
            JacsServiceData isotropicSamplingServiceData = vaa3dPluginProcessor.submit(new ServiceExecutionContext.Builder(jacsServiceData)
                            .state(JacsServiceState.RUNNING)
                            .build(),
                    new ServiceArg("-plugin", "ireg"),
                    new ServiceArg("-pluginFunc", "isampler"),
                    new ServiceArg("-input", workingSubjectFile.toString()),
                    new ServiceArg("-output", isotropicSubjectFile.toString()),
                    new ServiceArg("-pluginParams", String.format("#x %f", isx)),
                    new ServiceArg("-pluginParams", String.format("#y %f", isy)),
                    new ServiceArg("-pluginParams", String.format("#z %f", isz))
            );
            vaa3dPluginProcessor.execute(isotropicSamplingServiceData);
        } else {
            try {
                Files.createLink(isotropicSubjectFile, Paths.get(args.input1File));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private JacsServiceData resizeSubjectToTarget(AlignmentArgs args, JacsServiceData jacsServiceData) {
        JacsServiceData resizeSubjectServiceData = vaa3dPluginProcessor.submit(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "resizeImage"),
                new ServiceArg("-output", getWorkingResizedSubjectFile(args, jacsServiceData).toString()),
                new ServiceArg("-pluginParams", String.format("#s %s", getWorkingIsotropicSubjectFile(args, jacsServiceData))),
                new ServiceArg("-pluginParams", String.format("#t %s", getTargetExtFile(args))),
                new ServiceArg("-pluginParams", "#y 1")
        );
        vaa3dPluginProcessor.execute(resizeSubjectServiceData);
        return resizeSubjectServiceData;
    }

    private JacsServiceData extractRefFromSubject(AlignmentArgs args, JacsServiceData jacsServiceData, JacsServiceData ...waitFor) {
        JacsServiceData extractRefServiceData = vaa3dPluginProcessor.submit(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(JacsServiceState.RUNNING)
                        .waitFor(waitFor)
                        .build(),
                new ServiceArg("-plugin", "refExtract"),
                new ServiceArg("-pluginFunc", "refExtract"),
                new ServiceArg("-input", getWorkingResizedSubjectFile(args, jacsServiceData).toString()),
                new ServiceArg("-output", getWorkingRefChannelFromResizedSubjectFile(args, jacsServiceData).toString()),
                new ServiceArg("-pluginParams", String.format("#c %s", args.input1Ref))
        );
        vaa3dPluginProcessor.execute(extractRefServiceData);
        return extractRefServiceData;
    }

    @Override
    protected ServiceComputation<List<File>> processing(JacsServiceData jacsServiceData) {
        return createComputation(this.waitForResult(jacsServiceData));
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        AlignmentArgs args = getArgs(jacsServiceData);
        // count v3dpbd
        try {
            String resultsPattern = "glob:**/*.{v3dpbd}";
            PathMatcher inputFileMatcher =
                    FileSystems.getDefault().getPathMatcher(resultsPattern);
            long nFiles = Files.find(getResultsDir(args), 1, (p, a) -> inputFileMatcher.matches(p)).count();
            return nFiles > 0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected List<File> retrieveResult(JacsServiceData jacsServiceData) {
        AlignmentArgs args = getArgs(jacsServiceData);
        // collect all v3dpbd
        List<File> results = new ArrayList<>();
        try {
            String resultsPattern = "glob:**/*.{v3dpbd}";
            PathMatcher inputFileMatcher =
                    FileSystems.getDefault().getPathMatcher(resultsPattern);
            Files.find(getResultsDir(args), 1, (p, a) -> inputFileMatcher.matches(p)).forEach(p -> results.add(p.toFile()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return results;
    }

    private Path getResultsDir(AlignmentArgs args) {
        return Paths.get(args.resultsDir, com.google.common.io.Files.getNameWithoutExtension(args.input1File));
    }

    private Path getTargetExtFile(AlignmentArgs args) {
        return Paths.get(args.templateDir, args.targetExtTemplate);
    }

    private Path getNiftiTargetExtFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.targetExtTemplate) + "_c0.nii");
    }

    private Path getWorkingNeuronsFile(String neuronsFile, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(neuronsFile) + "Sx.v3draw");
    }

    private Path getWorkingSubjectFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + ".v3draw");
    }

    private Path getWorkingIsotropicSubjectFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "Is.v3draw");
    }

    private Path getWorkingResizedSubjectFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "Rs.v3draw");
    }

    private Path getWorkingRefChannelFromResizedSubjectFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "RsRefChn.v3draw");
    }

    private AlignmentArgs getArgs(JacsServiceData jacsServiceData) {
        return AlignmentArgs.parse(jacsServiceData.getArgsArray(), new AlignmentArgs());
    }

}
