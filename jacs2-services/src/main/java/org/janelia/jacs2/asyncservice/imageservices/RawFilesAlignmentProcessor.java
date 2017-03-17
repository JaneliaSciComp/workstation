package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
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
    private final FlirtProcessor flirtProcessor;

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
                               NiftiConverterProcessor niftiConverterProcessor,
                               FlirtProcessor flirtProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dConverterProcessor = vaa3dConverterProcessor;
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
        this.niftiConverterProcessor = niftiConverterProcessor;
        this.flirtProcessor = flirtProcessor;
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
        // convert the target to Nifti
        convertToNiftiImage(getTargetExtFile(args), getNiftiTargetExtFile(args, jacsServiceData), jacsServiceData);
        zFlipSubject(args, jacsServiceData);
        isotropicSubjectSampling(args, jacsServiceData);
        JacsServiceData resizeSubject = resizeSubjectToTarget(args, jacsServiceData);
        extractRefFromSubject(args, jacsServiceData, resizeSubject);
        // convert the ref channel to Nifti
        convertToNiftiImage(getWorkingResizedSubjectRefChannelFile(args, jacsServiceData), getNiftiResizedSubjectRefChannelFile(args, jacsServiceData), jacsServiceData);
        double downsampleFactor = 0.125;
        // downsample the target with ration 1/8
        JacsServiceData targetDownsampleServiceData = downsampleImage(
                getNiftiTargetExtFile(args, jacsServiceData),
                getNiftiTargetExtDownsampleFile(args, jacsServiceData),
                downsampleFactor,
                jacsServiceData);
        // downsample the subject with ration 1/8
        JacsServiceData subjectDownsampleServiceData = downsampleImage(
                getNiftiResizedSubjectRefChannelFile(args, jacsServiceData),
                getNiftiResizedSubjectRefChannelDownsampleFile(args, jacsServiceData),
                downsampleFactor,
                jacsServiceData);
        // get the rotations
        JacsServiceData estimateRotationsServiceData = findRotationMatrix(args, jacsServiceData, targetDownsampleServiceData, subjectDownsampleServiceData);
        JacsServiceData affinePrepServiceData = prepareAffineTransformation(args, jacsServiceData, estimateRotationsServiceData);
        // rotate the subject
        JacsServiceData rotateSubjectServiceData = rotateSubject(args, jacsServiceData, affinePrepServiceData);
        // convert rotated subject to Nifti
        convertToNiftiImage(getSubjectAffineRotationsMatrixFile(args, jacsServiceData), getNiftiRotatedSubjectRefChannelFile(args, jacsServiceData), jacsServiceData, rotateSubjectServiceData);

        return ImmutableList.of(); // FIXME!!!!
    }

    private JacsServiceData convertNeuronsFileToRawFormat(String neuronsFile, JacsServiceData jacsServiceData) {
        if ("v3draw".equals(com.google.common.io.Files.getFileExtension(neuronsFile))) {
            try {
                Files.createSymbolicLink(getWorkingNeuronsFile(neuronsFile, jacsServiceData), Paths.get(neuronsFile));
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            JacsServiceData convertToRawServiceData = submit(vaa3dConverterProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                            .state(JacsServiceState.RUNNING)
                            .build(),
                    new ServiceArg("-input", neuronsFile),
                    new ServiceArg("-output", getWorkingNeuronsFile(neuronsFile, jacsServiceData).toString())));
            vaa3dConverterProcessor.execute(convertToRawServiceData);
            return convertToRawServiceData;
        }
    }

    private JacsServiceData convertToNiftiImage(Path input, Path output, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Convert {} into a nifti image - {}", input, output);
        JacsServiceData niftiConverterServiceData = submit(niftiConverterProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-input", input.toString()),
                new ServiceArg("-output", output.toString())));
        niftiConverterProcessor.execute(niftiConverterServiceData);
        return niftiConverterServiceData;
    }

    private void zFlipSubject(AlignmentArgs args, JacsServiceData jacsServiceData) {
        if (args.zFlip) {
            logger.info("Flip {} along the z axis", args.input1File);
            JacsServiceData zFlipSubjectsServiceData = submit(vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                            .state(JacsServiceState.RUNNING)
                            .build(),
                    new ServiceArg("-plugin", "ireg"),
                    new ServiceArg("-pluginFunc", "zflip"),
                    new ServiceArg("-input", args.input1File),
                    new ServiceArg("-output", getWorkingSubjectFile(args, jacsServiceData).toString())));
            vaa3dPluginProcessor.execute(zFlipSubjectsServiceData);
        } else {
            try {
                Files.createSymbolicLink(getWorkingSubjectFile(args, jacsServiceData), Paths.get(args.input1File));
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
            JacsServiceData isotropicSamplingServiceData = submit(vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                            .state(JacsServiceState.RUNNING)
                            .build(),
                    new ServiceArg("-plugin", "ireg"),
                    new ServiceArg("-pluginFunc", "isampler"),
                    new ServiceArg("-input", workingSubjectFile.toString()),
                    new ServiceArg("-output", isotropicSubjectFile.toString()),
                    new ServiceArg("-pluginParams", String.format("#x %f", isx)),
                    new ServiceArg("-pluginParams", String.format("#y %f", isy)),
                    new ServiceArg("-pluginParams", String.format("#z %f", isz))
            ));
            vaa3dPluginProcessor.execute(isotropicSamplingServiceData);
        } else {
            try {
                Files.createSymbolicLink(isotropicSubjectFile, Paths.get(args.input1File));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private JacsServiceData resizeSubjectToTarget(AlignmentArgs args, JacsServiceData jacsServiceData) {
        JacsServiceData resizeSubjectServiceData = submit(vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "resizeImage"),
                new ServiceArg("-output", getWorkingResizedSubjectFile(args, jacsServiceData).toString()),
                new ServiceArg("-pluginParams", String.format("#s %s", getWorkingIsotropicSubjectFile(args, jacsServiceData))),
                new ServiceArg("-pluginParams", String.format("#t %s", getTargetExtFile(args))),
                new ServiceArg("-pluginParams", "#y 1")
        ));
        vaa3dPluginProcessor.execute(resizeSubjectServiceData);
        return resizeSubjectServiceData;
    }

    private JacsServiceData extractRefFromSubject(AlignmentArgs args, JacsServiceData jacsServiceData, JacsServiceData ...waitFor) {
        JacsServiceData extractRefServiceData = submit(vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(JacsServiceState.RUNNING)
                        .waitFor(waitFor)
                        .build(),
                new ServiceArg("-plugin", "refExtract"),
                new ServiceArg("-pluginFunc", "refExtract"),
                new ServiceArg("-input", getWorkingResizedSubjectFile(args, jacsServiceData).toString()),
                new ServiceArg("-output", getWorkingResizedSubjectRefChannelFile(args, jacsServiceData).toString()),
                new ServiceArg("-pluginParams", String.format("#c %s", args.input1Ref))
        ));
        vaa3dPluginProcessor.execute(extractRefServiceData);
        return extractRefServiceData;
    }

    private JacsServiceData downsampleImage(Path input, Path output, double downsampleFactor, JacsServiceData jacsServiceData) {
        logger.info("Downsample {} -> {}", input, output);
        JacsServiceData downsampleServiceData = submit(vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "resamplebyspacing"),
                new ServiceArg("-input", input.toString()),
                new ServiceArg("-output", output.toString()),
                new ServiceArg("-pluginParams", String.format("#x %f", downsampleFactor)),
                new ServiceArg("-pluginParams", String.format("#y %f", downsampleFactor)),
                new ServiceArg("-pluginParams", String.format("#z %f", downsampleFactor))
        ));
        vaa3dPluginProcessor.execute(downsampleServiceData);
        return downsampleServiceData;
    }

    private JacsServiceData findRotationMatrix(AlignmentArgs args, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        Path rotationsMatFile = getSubjectRotationsMatrixFile(args, jacsServiceData);
        logger.info("Find rotations {}", rotationsMatFile);
        JacsServiceData rotateServiceData = submit(flirtProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(JacsServiceState.RUNNING)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-in", getNiftiResizedSubjectRefChannelDownsampleFile(args, jacsServiceData).toString()),
                new ServiceArg("-ref", getNiftiTargetExtDownsampleFile(args, jacsServiceData).toString()),
                new ServiceArg("-omat", rotationsMatFile.toString()),
                new ServiceArg("-cost", "mutualinfo"),
                new ServiceArg("-searchrx", 2, "-180", "180"),
                new ServiceArg("-searchry", 2, "-180", "180"),
                new ServiceArg("-searchrz", 2, "-180", "180"),
                new ServiceArg("-dof", "12"),
                new ServiceArg("-datatype", "char"),
                new ServiceArg("-fslOutputType", args.fslOutputType)
        ));
        flirtProcessor.execute(rotateServiceData);
        return rotateServiceData;
    }

    private JacsServiceData prepareAffineTransformation(AlignmentArgs args, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        Path rotationsMatFile = getSubjectRotationsMatrixFile(args, jacsServiceData);
        Path insightRotationsFile = getSubjectInsightRotationsMatrixFile(args, jacsServiceData);
        Path affineRotationsFile = getSubjectAffineRotationsMatrixFile(args, jacsServiceData);

        logger.info("Estimate rotations {}", rotationsMatFile);
        AlignmentUtils.convertAffineMatToInsightMat(rotationsMatFile, insightRotationsFile);
        JacsServiceData estimateRotationsServiceData = submit(vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .description("Estimate rotations")
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "extractRotMat"),
                new ServiceArg("-input", insightRotationsFile.toString()),
                new ServiceArg("-output", affineRotationsFile.toString())
        ));
        vaa3dPluginProcessor.execute(estimateRotationsServiceData);
        return estimateRotationsServiceData;
    }

    private JacsServiceData rotateSubject(AlignmentArgs args, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        Path subjectFile = getWorkingResizedSubjectRefChannelFile(args, jacsServiceData);
        logger.info("Rotate subject {}", subjectFile);
        JacsServiceData estimateRotationsServiceData = submit(vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .description("Rotate subject")
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "warp"),
                new ServiceArg("-output", getRotatedSubjectRefChannelFile(args, jacsServiceData).toString()),
                new ServiceArg("-pluginParams", String.format("#s %s", subjectFile)),
                new ServiceArg("-pluginParams", String.format("#t %s", getTargetExtFile(args))),
                new ServiceArg("-pluginParams", String.format("#a %s", getSubjectAffineRotationsMatrixFile(args, jacsServiceData)))
        ));
        vaa3dPluginProcessor.execute(estimateRotationsServiceData);
        return estimateRotationsServiceData;
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

    private Path getNiftiTargetExtDownsampleFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.targetExtTemplate) + "_ds.nii");
    }

    private Path getWorkingNeuronsFile(String neuronsFile, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(neuronsFile) + "-Sx.v3draw");
    }

    private Path getWorkingSubjectFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + ".v3draw");
    }

    private Path getWorkingIsotropicSubjectFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-Is.v3draw");
    }

    private Path getWorkingResizedSubjectFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-Rs.v3draw");
    }

    private Path getWorkingResizedSubjectRefChannelFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-RsRefChn.v3draw");
    }

    private Path getNiftiResizedSubjectRefChannelFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-RsRefChn_c0.nii");
    }

    private Path getNiftiResizedSubjectRefChannelDownsampleFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "RsRefChn_ds.nii");
    }

    private Path getSubjectRotationsMatrixFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-Rotations.mat");
    }

    private Path getSubjectInsightRotationsMatrixFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-Rotations.txt");
    }

    private Path getSubjectAffineRotationsMatrixFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-RotationsAffine.mat");
    }

    private Path getRotatedSubjectRefChannelFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-RsRefChnRot.v3draw");
    }

    private Path getNiftiRotatedSubjectRefChannelFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-RsRefChnRot_c0.nii");
    }

    private AlignmentArgs getArgs(JacsServiceData jacsServiceData) {
        return AlignmentArgs.parse(jacsServiceData.getArgsArray(), new AlignmentArgs());
    }

}
