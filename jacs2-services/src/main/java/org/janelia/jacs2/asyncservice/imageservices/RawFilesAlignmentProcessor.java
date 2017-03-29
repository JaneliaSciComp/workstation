package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractFileListServiceResultHandler;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentConfiguration;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentUtils;
import org.janelia.jacs2.asyncservice.imageservices.align.ImageCoordinates;
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RawFilesAlignmentProcessor executes the alignment "pipeline" and assumes all files are already
 * in v3draw format.
 */
@Named("alignRaw")
public class RawFilesAlignmentProcessor extends AbstractBasicLifeCycleServiceProcessor<List<File>> {

    private static final String MAX_AFFINE_ITERATIONS = "10000x10000x10000x10000";
    private static final String MAX_CC_ITERATIONS ="100x70x50x0x0" ;
    private static final int TARSXEXTDX = 1933;
    private static final int TARSXEXTDY = 1713;
    private static final int TARSXEXTDZ = 640;

    static class AlignmentArgs extends ServiceArgs {
        @Parameter(names = {"-nthreads"}, description = "Number of ITK threads")
        Integer nthreads = 16;
        @Parameter(names = "-i1File", description = "The name of the first input file", required = true)
        String input1File;
        @Parameter(names = "-i1Channels", description = "The channels of the first input file", required = true)
        int input1Channels;
        @Parameter(names = "-i1Ref", description = "The reference for the first input file", required = true)
        int input1Ref;
        @Parameter(names = "-i1Res", description = "The resolution of the first input file", required = true)
        String input1Res;
        @Parameter(names = "-i1Dims", description = "The dimensions of the first input file", required = false)
        String input1Dims;
        @Parameter(names = {"-e", "-i1Neurons"}, description = "Input1 neurons file", required = false)
        String input1Neurons;
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
    private final AntsToolProcessor antsToolProcessor;
    private final WarpToolProcessor warpToolProcessor;

    @Inject
    RawFilesAlignmentProcessor(ServiceComputationFactory computationFactory,
                               JacsServiceDataPersistence jacsServiceDataPersistence,
                               @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                               @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                               Vaa3dConverterProcessor vaa3dConverterProcessor,
                               Vaa3dPluginProcessor vaa3dPluginProcessor,
                               NiftiConverterProcessor niftiConverterProcessor,
                               FlirtProcessor flirtProcessor,
                               AntsToolProcessor antsToolProcessor,
                               WarpToolProcessor warpToolProcessor,
                               Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dConverterProcessor = vaa3dConverterProcessor;
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
        this.niftiConverterProcessor = niftiConverterProcessor;
        this.flirtProcessor = flirtProcessor;
        this.antsToolProcessor = antsToolProcessor;
        this.warpToolProcessor = warpToolProcessor;
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
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                return areAllDependenciesDone(jacsServiceData);
            }

            @Override
            public List<File> collectResult(JacsServiceData jacsServiceData) {
                AlignmentArgs args = getArgs(jacsServiceData);
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
        } catch (Exception e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return jacsServiceData;
    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        AlignmentArgs args = getArgs(jacsServiceData);
        AlignmentConfiguration alignConfig = AlignmentUtils.parseAlignConfig(args.configFile);
        ImageCoordinates inputResolution = AlignmentUtils.parseCoordinates(args.input1Res);

        JacsServiceData jacsServiceDataHierarchy = jacsServiceDataPersistence.findServiceHierarchy(jacsServiceData.getId());

        Path subjectFile = getWorkingSubjectFile(args, jacsServiceDataHierarchy); // => SUBSX
        Path labelsFile = getWorkingLabelsFile(args, jacsServiceDataHierarchy); // => SUBSXNEURONS
        Path isotropicSubjectFile = getWorkingIsotropicSubjectFile(args, jacsServiceDataHierarchy); // => SUBSXIS
        Path targetFile = getWorkingTargetFile(args, jacsServiceDataHierarchy); // => TARSX
        Path targetExtFile = getWorkingTargetExtFile(args, jacsServiceDataHierarchy); // => TARSXEXT
        Path targetExtNiftiFile = getNiftiTargetExtFile(args, jacsServiceDataHierarchy); // => TARSXNII
        Path targetExtDownsampledFile = getNiftiTargetExtDownsampleFile(args, jacsServiceDataHierarchy); // => FDS
        Path resizedSubjectFile = getWorkingResizedSubjectFile(args, jacsServiceDataHierarchy); // => SUBSXRS
        Path resizedSubjectRefFile = getWorkingResizedSubjectRefChannelFile(args, jacsServiceDataHierarchy); // => SUBSXRSRFC
        Path resizedSubjectRefNiftiFile = getNiftiResizedSubjectRefChannelFile(args, jacsServiceDataHierarchy); // => SUBSXRSRFCNII
        Path resizedSubjectRefDownsampledFile = getNiftiResizedSubjectRefChannelDownsampleFile(args, jacsServiceDataHierarchy); // => MDS
        Path rotationsMatFile = getSubjectRotationsMatrixFile(args, jacsServiceDataHierarchy); // => RCMAT
        Path insightRotationsFile = getSubjectInsightRotationsMatrixFile(args, jacsServiceDataHierarchy); // => RCOUT
        Path rotationsAffineFile = getSubjectAffineRotationsMatrixFile(args, jacsServiceDataHierarchy); // => $RCAFFINE
        Path rotatedSubjectFile = getRotatedSubjectRefChannelFile(args, jacsServiceDataHierarchy); // => SUBSXRFCROT
        Path rotatedSubjectNiftiFile = getNiftiRotatedSubjectRefChannelFile(args, jacsServiceDataHierarchy); // => SUBNII
        Path globalSymmetricTransformFile = getGlobalSymmetricTransformFilePrefix(args, jacsServiceDataHierarchy); // => SIMMETRIC txmi
        Path rotatedSubjectRecenteredFile = getWorkingResizedRotatedRecenteredSubjectFile(args, jacsServiceDataHierarchy); // => SUBSXRSROT
        Path symmetricAffineTransformFile = getSymmetricAffineTransformFile(args, jacsServiceDataHierarchy); // => AFFINEMATRIX
        Path rotatedSubjectGlobalAllignedFile = getRotatedGlobalAlignedSubjectRefChannelFile(args, jacsServiceDataHierarchy); // => SUBSXRSROTGA
        Path resizedSubjectGlobalAlignedFile = getResizedGlobalAlignedSubjectRefChannelFile(args, jacsServiceDataHierarchy); // => SUBSXRSROTGARS
        List<Path> resizedSubjectGlobalAlignedNiftiFiles = getNiftiResizedGlobalAlignedSubjectChannelFiles(args, args.input1Channels, jacsServiceDataHierarchy); // => MOVINGNIICI, MOVINGNIICII, MOVINGNIICIII, MOVINGNIICIV
        Path resizedTargetFile = getResizedTargetFile(args, jacsServiceDataHierarchy); // => TARSXRS
        Path resizedTargetNiftiFile = getNiftiResizedTargetFile(args, jacsServiceDataHierarchy); // => FIXEDNII, FIX
        Path localSymmetricTransformFilePrefix = getLocalSymmetricTransformFilePrefix(args, jacsServiceDataHierarchy); // => SIMMETRIC ccmi
        Path resizedSubjectGlobalAlignedRefChannelNiftiFile = getNiftiResizedGlobalAlignedSubjectChannelFile(args, args.input1Ref - 1, jacsServiceDataHierarchy); // => MOVINGNIICR, MOV
        Path localAffineTransformFile = getLocalSymmetricAffineTransformFile(args, jacsServiceDataHierarchy); // => AFFINEMATRIXLOCAL
        Path localWarpFile = getLocalWarpFile(args, jacsServiceDataHierarchy); // => FWDDISPFIELD

        createWorkingCopy(Paths.get(args.templateDir, args.targetTemplate), targetFile);
        createWorkingCopy(Paths.get(args.templateDir, args.targetExtTemplate), targetExtFile);
        // ensureRawFileWdiffName "$Vaa3D" "$WORKDIR" "$SUBSXNEURONS" "${SUBSXNEURONS%.*}_SX.v3draw" SUBSXNEURONS
        JacsServiceData neuronsToRawServiceData = convertNeuronsFileToRawFormat(Paths.get(args.input1Neurons), labelsFile, jacsServiceDataHierarchy);
        // convert the target to Nifti
        // $Vaa3D -x ireg -f NiftiImageConverter -i $TARSXEXT
        JacsServiceData targetExtToNiftiServiceData = convertToNiftiImage(targetExtFile, targetExtNiftiFile, jacsServiceDataHierarchy);
        //  $Vaa3D -x ireg -f zflip -i ${SUBSX} -o ${TEMPSUBJECT}
        JacsServiceData flippedSubjectServiceData = zFlip(Paths.get(args.input1File), subjectFile, args.zFlip, jacsServiceDataHierarchy);
        // $Vaa3D -x ireg -f isampler -i $SUBSX -o $SUBSXIS -p "#x $ISRX #y $ISRY #z $ISRZ"
        JacsServiceData isotropicSamplingServiceData = isotropicSubjectSampling(subjectFile, alignConfig, inputResolution, isotropicSubjectFile, jacsServiceDataHierarchy, flippedSubjectServiceData);
        JacsServiceData resizeSubjectServiceData = resizeSubjectToTarget(isotropicSubjectFile, targetExtFile, resizedSubjectFile, jacsServiceDataHierarchy, isotropicSamplingServiceData);
        JacsServiceData extractRefChannelServiceData = extractRefFromSubject(resizedSubjectFile, resizedSubjectRefFile, args.input1Ref, jacsServiceDataHierarchy, resizeSubjectServiceData);
        // convert the ref channel to Nifti
        JacsServiceData resizedSubjectToNiftiServiceData = convertToNiftiImage(resizedSubjectRefFile, resizedSubjectRefNiftiFile, jacsServiceDataHierarchy, extractRefChannelServiceData);
        double downsampleFactor = 0.125;
        // downsample the target with ration 1/8
        JacsServiceData targetDownsampleServiceData = downsampleImage(targetExtNiftiFile, targetExtDownsampledFile, downsampleFactor, jacsServiceDataHierarchy,
                targetExtToNiftiServiceData);
        // downsample the subject with ration 1/8
        JacsServiceData subjectDownsampleServiceData = downsampleImage(resizedSubjectRefNiftiFile, resizedSubjectRefDownsampledFile,  downsampleFactor, jacsServiceDataHierarchy,
                resizedSubjectToNiftiServiceData);
        // find the rotations with FLIRT
        // $FLIRT -in $MDS -ref $FDS -omat $RCMAT -cost mutualinfo -searchrx -180 180 -searchry -180 180 -searchrz -180 180 -dof 12 -datatype char
        JacsServiceData estimateRotationsServiceData = findRotationMatrix(resizedSubjectRefDownsampledFile, targetExtDownsampledFile, rotationsMatFile, args.fslOutputType,
                jacsServiceDataHierarchy, targetDownsampleServiceData, subjectDownsampleServiceData);
        JacsServiceData affinePrepServiceData = prepareAffineTransformation(rotationsMatFile, insightRotationsFile, rotationsAffineFile, jacsServiceDataHierarchy, estimateRotationsServiceData);
        // rotate the subject
        JacsServiceData rotateSubjectServiceData = rotateSubject(resizedSubjectRefFile, targetExtFile, rotationsAffineFile, rotatedSubjectFile, jacsServiceDataHierarchy, affinePrepServiceData);
        // convert rotated subject to Nifti
        JacsServiceData subjectToNiftiServiceData = convertToNiftiImage(rotatedSubjectFile, rotatedSubjectNiftiFile, jacsServiceDataHierarchy, rotateSubjectServiceData);
        // global alignment of the subject to target
        JacsServiceData globalAlignServiceData = globalAlignSubjectToTarget(targetExtNiftiFile, rotatedSubjectNiftiFile, globalSymmetricTransformFile, jacsServiceDataHierarchy, subjectToNiftiServiceData);
        // rotate recentered object
        JacsServiceData rotateRecenteredServiceData = applyIWarp2Transformation(resizedSubjectFile, rotationsAffineFile, rotatedSubjectRecenteredFile, jacsServiceDataHierarchy, resizeSubjectServiceData);
        // affine transform rotated subject
        JacsServiceData globalAlignedServiceData = applyIWarp2Transformation(rotatedSubjectRecenteredFile, symmetricAffineTransformFile, rotatedSubjectGlobalAllignedFile, jacsServiceDataHierarchy, rotateRecenteredServiceData, globalAlignServiceData);
        JacsServiceData voiServiceData = getVOI(rotatedSubjectGlobalAllignedFile, targetFile, jacsServiceDataHierarchy, globalAlignedServiceData);
        JacsServiceData resizedAlignedSubjectToNiftiServiceData = convertToNiftiImage(resizedSubjectGlobalAlignedFile, resizedSubjectGlobalAlignedNiftiFiles, jacsServiceDataHierarchy, voiServiceData);
        JacsServiceData resizedTargetToNiftiServiceData = convertToNiftiImage(resizedTargetFile, resizedTargetNiftiFile, jacsServiceDataHierarchy, voiServiceData);
        JacsServiceData localAlignServiceData = localAlignSubject(resizedTargetNiftiFile, resizedSubjectGlobalAlignedRefChannelNiftiFile, localSymmetricTransformFilePrefix, jacsServiceDataHierarchy, resizedAlignedSubjectToNiftiServiceData, resizedTargetToNiftiServiceData);
        List<JacsServiceData> warpServices = new ArrayList<>();
        List<Path> warpedFiles = new ArrayList<>();
        for (int channelNo = 0; channelNo < args.input1Channels; channelNo++) {
            Path inputMovingFile = getNiftiResizedGlobalAlignedSubjectChannelFile(args, channelNo, jacsServiceDataHierarchy); // => MOVINGNIICI, MOVINGNIICII, MOVINGNIICIII, MOVINGNIICIV
            Path warpedFile = getNiftiResizedGlobalAlignedDeformedSubjectChannelFile(args, channelNo, jacsServiceDataHierarchy); // => MOVINGDFRMDCI, MOVINGDFRMDCII, MOVINGDFRMDCIII, MOVINGDFRMDCIV
            warpedFiles.add(warpedFile);
            JacsServiceData warpServiceData = warp(inputMovingFile, warpedFile, ImmutableList.of(resizedTargetNiftiFile, localWarpFile, localAffineTransformFile), jacsServiceDataHierarchy, localAlignServiceData);
            if (warpServiceData != null) {
                warpServices.add(warpServiceData);
            }
        }
        Path resizedAlignedSubjectFile = getResizedAlignedFile(args, jacsServiceDataHierarchy); // => SUBSXDFRMD
        Path alignedSubjectFile = getAlignedFile(args, jacsServiceDataHierarchy); // => SUBSXALINGED
        JacsServiceData combineChannelsServiceData = convertToNiftiImage(warpedFiles, resizedAlignedSubjectFile, "#b 1 #v 1", jacsServiceDataHierarchy, warpServices.toArray(new JacsServiceData[warpServices.size()]));
        // resize to the templates' space
        JacsServiceData resizedAlignedSubject = resizeSubjectToTarget(resizedAlignedSubjectFile, targetFile, alignedSubjectFile, jacsServiceDataHierarchy, combineChannelsServiceData);
        return ImmutableList.of(resizedAlignedSubject); // FIXME!!!!
    }

    private void createWorkingCopy(Path inputFile, Path outputFile) {
        try {
            Path outputDir = outputFile.getParent();
            Files.createDirectories(outputDir);
            Files.createSymbolicLink(outputFile, inputFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JacsServiceData convertNeuronsFileToRawFormat(Path neuronsFile, Path labelsFile, JacsServiceData jacsServiceData) {
        if ("v3draw".equals(com.google.common.io.Files.getFileExtension(neuronsFile.toString()))) {
            try {
                Files.createSymbolicLink(labelsFile, neuronsFile);
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            JacsServiceData convertToRawServiceData = vaa3dConverterProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                            .state(JacsServiceState.RUNNING)
                            .build(),
                    new ServiceArg("-input", neuronsFile.toString()),
                    new ServiceArg("-output", labelsFile.toString())
            );
            return submitDependencyIfNotPresent(jacsServiceData, convertToRawServiceData);
        }
    }

    private JacsServiceData convertToNiftiImage(Path input, Path output, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Convert {} into a nifti image - {}", input, output);
        if (output.toFile().exists()) {
            return null;
        }
        JacsServiceData niftiConverterServiceData = niftiConverterProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-input", input.toString()),
                new ServiceArg("-output", output == null ? null : output.toString()) // generate the default output
        );
        return submitDependencyIfNotPresent(jacsServiceData, niftiConverterServiceData);
    }

    private JacsServiceData convertToNiftiImage(Path input, List<Path> outputs, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Convert {} into a nifti image - {}", input, outputs);
        if (!outputs.isEmpty() && outputs.stream().reduce(true, (b, p) -> b && p.toFile().exists(), (b1, b2) -> b1 && b2)) {
            return null;
        }
        JacsServiceData niftiConverterServiceData = niftiConverterProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-input", input.toString()),
                new ServiceArg("-output", outputs.stream().map(Path::toString).collect(Collectors.joining(",")))
        );
        return submitDependencyIfNotPresent(jacsServiceData, niftiConverterServiceData);
    }

    private JacsServiceData convertToNiftiImage(List<Path> inputs, Path output, String otherParams, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Convert {} into a nifti image - {}", inputs, output);
        if (output.toFile().exists()) {
            return null;
        }
        JacsServiceData niftiConverterServiceData = niftiConverterProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-input", inputs.stream().map(Path::toString).collect(Collectors.joining(","))),
                new ServiceArg("-output", output.toString()),
                new ServiceArg("-pluginParams", otherParams)
        );
        return submitDependencyIfNotPresent(jacsServiceData, niftiConverterServiceData);
    }

    private JacsServiceData zFlip(Path inputFile, Path outputFile, boolean zFlip, JacsServiceData jacsServiceData) {
        if (zFlip) {
            logger.info("Flip {} along the z axis", inputFile);
            if (outputFile.toFile().exists()) {
                return null;
            }
            JacsServiceData zFlipSubjectsServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                            .state(JacsServiceState.RUNNING)
                            .build(),
                    new ServiceArg("-plugin", "ireg"),
                    new ServiceArg("-pluginFunc", "zflip"),
                    new ServiceArg("-input", inputFile.toString()),
                    new ServiceArg("-output", outputFile.toString())
            );
            return submitDependencyIfNotPresent(jacsServiceData, zFlipSubjectsServiceData);
        } else {
            try {
                Files.createSymbolicLink(outputFile, inputFile);
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private JacsServiceData isotropicSubjectSampling(Path subjectFile, AlignmentConfiguration alignConfig, ImageCoordinates imageResolution, Path isotropicSubjectFile, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        if (isotropicSubjectFile.toFile().exists()) {
            return null;
        }
        double isx = imageResolution.x / alignConfig.misc.vSzIsX63x;
        double isy = imageResolution.y / alignConfig.misc.vSzIsY63x;
        double isz = imageResolution.z / alignConfig.misc.vSzIsZ63x;
        if (Math.abs(isx - 1.) >  0.01
                || Math.abs(isy - 1.) >  0.01
                || Math.abs(isz - 1.) >  0.01) {
            JacsServiceData isotropicSamplingServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                            .waitFor(deps)
                            .state(JacsServiceState.RUNNING)
                            .description("Isotropic sampling 63x subject")
                            .build(),
                    new ServiceArg("-plugin", "ireg"),
                    new ServiceArg("-pluginFunc", "isampler"),
                    new ServiceArg("-input", subjectFile.toString()),
                    new ServiceArg("-output", isotropicSubjectFile.toString()),
                    new ServiceArg("-pluginParams", String.format("#x %f", isx)),
                    new ServiceArg("-pluginParams", String.format("#y %f", isy)),
                    new ServiceArg("-pluginParams", String.format("#z %f", isz))
            );
            return submitDependencyIfNotPresent(jacsServiceData, isotropicSamplingServiceData);
        } else {
            try {
                Files.createSymbolicLink(isotropicSubjectFile, subjectFile);
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private JacsServiceData resizeSubjectToTarget(Path subjectFile, Path targetFile, Path resizedSubjectFile, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        if (resizedSubjectFile.toFile().exists()) {
            return null;
        }
        JacsServiceData resizeSubjectServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "resizeImage"),
                new ServiceArg("-output", resizedSubjectFile.toString()),
                new ServiceArg("-pluginParams", String.format("#s %s", subjectFile)),
                new ServiceArg("-pluginParams", String.format("#t %s", targetFile)),
                new ServiceArg("-pluginParams", "#y 1")
        );
        return submitDependencyIfNotPresent(jacsServiceData, resizeSubjectServiceData);
    }

    private JacsServiceData extractRefFromSubject(Path resizedSubjectFile, Path resizedSubjectRefFile, int refChannel, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        if (resizedSubjectRefFile.toFile().exists()) {
            return null;
        }
        JacsServiceData extractRefServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-plugin", "refExtract"),
                new ServiceArg("-pluginFunc", "refExtract"),
                new ServiceArg("-input", resizedSubjectFile.toString()),
                new ServiceArg("-output", resizedSubjectRefFile.toString()),
                new ServiceArg("-pluginParams", String.format("#c %d", refChannel))
        );
        return submitDependencyIfNotPresent(jacsServiceData, extractRefServiceData);
    }

    private JacsServiceData downsampleImage(Path input, Path output, double downsampleFactor, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        if (output.toFile().exists()) {
            return null;
        }
        logger.info("Downsample {} -> {}", input, output);
        JacsServiceData downsampleServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "resamplebyspacing"),
                new ServiceArg("-input", input.toString()),
                new ServiceArg("-output", output.toString()),
                new ServiceArg("-pluginParams", String.format("#x %f", downsampleFactor)),
                new ServiceArg("-pluginParams", String.format("#y %f", downsampleFactor)),
                new ServiceArg("-pluginParams", String.format("#z %f", downsampleFactor))
        );
        return submitDependencyIfNotPresent(jacsServiceData, downsampleServiceData);
    }

    private JacsServiceData findRotationMatrix(Path resizedSubjectRefDownsampledFile, Path targetExtDownsampledFile, Path rotationsMatFile, String fslOutputType, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Find rotations {}", rotationsMatFile);
        JacsServiceData rotateServiceData = flirtProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(JacsServiceState.RUNNING)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-in", resizedSubjectRefDownsampledFile.toString()),
                new ServiceArg("-ref", targetExtDownsampledFile.toString()),
                new ServiceArg("-omat", rotationsMatFile.toString()),
                new ServiceArg("-cost", "mutualinfo"),
                new ServiceArg("-searchrx", 2, "-180", "180"),
                new ServiceArg("-searchry", 2, "-180", "180"),
                new ServiceArg("-searchrz", 2, "-180", "180"),
                new ServiceArg("-dof", "12"),
                new ServiceArg("-datatype", "char"),
                new ServiceArg("-fslOutputType", fslOutputType)
        );
        return submitDependencyIfNotPresent(jacsServiceData, rotateServiceData);
    }

    private JacsServiceData prepareAffineTransformation(Path rotationsMatFile, Path insightRotationsFile, Path rotationsAffineFile, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Estimate rotations {}", rotationsMatFile);
        AlignmentUtils.convertAffineMatToInsightMat(rotationsMatFile, insightRotationsFile);
        JacsServiceData estimateRotationsServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .description("Estimate rotations")
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "extractRotMat"),
                new ServiceArg("-input", insightRotationsFile.toString()),
                new ServiceArg("-output", rotationsAffineFile.toString())
        );
        return submitDependencyIfNotPresent(jacsServiceData, estimateRotationsServiceData);
    }

    private JacsServiceData rotateSubject(Path resizedSubjectRefFile, Path targetExtFile, Path transformationsFile, Path rotatedSubjectFile, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        JacsServiceData estimateRotationsServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .description("Rotate subject")
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "iwarp"),
                new ServiceArg("-output", rotatedSubjectFile.toString()),
                new ServiceArg("-pluginParams", String.format("#s %s", resizedSubjectRefFile)),
                new ServiceArg("-pluginParams", String.format("#t %s", targetExtFile)),
                new ServiceArg("-pluginParams", String.format("#a %s", transformationsFile))
        );
        return submitDependencyIfNotPresent(jacsServiceData, estimateRotationsServiceData);
    }

    private JacsServiceData globalAlignSubjectToTarget(Path targetExtNiftiFile, Path rotatedSubjectNiftiFile, Path symmetricTransformFile, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Align subject to target");
        JacsServiceData alignSubjectToTargetServiceData = antsToolProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-dims", "3"),
                new ServiceArg("-metric",
                        String.format("MI[%s, %s, %d, %d]",
                                targetExtNiftiFile,
                                rotatedSubjectNiftiFile,
                                1,
                                32)),
                new ServiceArg("-output", symmetricTransformFile.toString()),
                new ServiceArg("-iterations", "0"),
                new ServiceArg("-affineIterations", MAX_AFFINE_ITERATIONS)
        );
        return submitDependencyIfNotPresent(jacsServiceData, alignSubjectToTargetServiceData);
    }

    private JacsServiceData applyIWarp2Transformation(Path subjectFile, Path transformationFile, Path outputFile, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Apply transformation {} to {} => {}", transformationFile, subjectFile, outputFile);
        JacsServiceData estimateRotationsServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .description("Rotate subject")
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "iwarp2"),
                new ServiceArg("-output", outputFile.toString()),
                new ServiceArg("-pluginParams", String.format("#s %s", subjectFile)),
                new ServiceArg("-pluginParams", String.format("#a %s", transformationFile)),
                new ServiceArg("-pluginParams", String.format("#dx %d", TARSXEXTDX)),
                new ServiceArg("-pluginParams", String.format("#dy %s", TARSXEXTDY)),
                new ServiceArg("-pluginParams", String.format("#dz %s", TARSXEXTDZ))
        );
        return submitDependencyIfNotPresent(jacsServiceData, estimateRotationsServiceData);
    }

    private JacsServiceData getVOI(Path subjectFile, Path targetFile, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Resize subject {} to original target {}", subjectFile, targetFile);
        JacsServiceData resizeSubjectServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .description("Resize subject to original data")
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "genVOIs"),
                new ServiceArg("-pluginParams", String.format("#s %s", subjectFile)),
                new ServiceArg("-pluginParams", String.format("#t %s", targetFile))
        );
        return submitDependencyIfNotPresent(jacsServiceData, resizeSubjectServiceData);
    }

    private JacsServiceData localAlignSubject(Path targetNiftiFile, Path refChannelSubjectNiftiFile, Path symmetricTransformFile, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Align subject to target");
        JacsServiceData alignSubjectToTargetServiceData = antsToolProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-dims", "3"),
                new ServiceArg("-metric",
                        String.format("CC[%s, %s, %d, %d]",
                                targetNiftiFile,
                                refChannelSubjectNiftiFile,
                                1,
                                8)),
                new ServiceArg("-transformationModel", String.format("SyN[%f]", 0.25)),
                new ServiceArg("-roi", String.format("Gauss[%d,%d]", 3, 0)),
                new ServiceArg("-output", symmetricTransformFile.toString()),
                new ServiceArg("-iterations", MAX_CC_ITERATIONS)
        );
        return submitDependencyIfNotPresent(jacsServiceData, alignSubjectToTargetServiceData);
    }

    private JacsServiceData warp(Path input, Path output, List<Path> references, JacsServiceData jacsServiceData, JacsServiceData... deps) {
        JacsServiceData warpServiceData = warpToolProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .state(JacsServiceState.RUNNING)
                        .build(),
                new ServiceArg("-dims", "3"),
                new ServiceArg("-i", input.toString()),
                new ServiceArg("-o", output.toString()),
                new ServiceArg("-r", references.stream().map(Path::toString).collect(Collectors.joining(","))),
                new ServiceArg("-bspline")
        );
        return submitDependencyIfNotPresent(jacsServiceData, warpServiceData);
    }

    @Override
    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(jacsServiceData);
    }

    private AlignmentArgs getArgs(JacsServiceData jacsServiceData) {
        return AlignmentArgs.parse(jacsServiceData.getArgsArray(), new AlignmentArgs());
    }

    private Path getResultsDir(AlignmentArgs args) {
        return Paths.get(args.resultsDir, com.google.common.io.Files.getNameWithoutExtension(args.input1File));
    }

    private Path getWorkingTargetFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.targetTemplate) + ".v3draw");
    }

    private Path getWorkingTargetExtFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.targetExtTemplate) + ".v3draw");
    }

    private Path getNiftiTargetExtFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.targetExtTemplate) + "_c0.nii");
    }

    private Path getNiftiTargetExtDownsampleFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.targetExtTemplate) + "_ds.nii");
    }

    private Path getWorkingLabelsFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1Neurons) + "-Sx.v3draw");
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

    private Path getWorkingResizedRotatedRecenteredSubjectFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-RsRotated.v3draw");
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
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-RotationsAffine.txt");
    }

    private Path getRotatedSubjectRefChannelFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-RsRefChnRot.v3draw");
    }

    private Path getNiftiRotatedSubjectRefChannelFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-RsRefChnRot_c0.nii");
    }

    private Path getGlobalSymmetricTransformFilePrefix(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-txmi");
    }

    private Path getSymmetricAffineTransformFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-txmiAffine.txt");
    }

    private Path getRotatedGlobalAlignedSubjectRefChannelFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-RsRotGlobalAligned.v3draw");
    }

    private Path getResizedGlobalAlignedSubjectRefChannelFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-RsRotGlobalAligned_rs.v3draw");
    }

    private List<Path> getNiftiResizedGlobalAlignedSubjectChannelFiles(AlignmentArgs args, int nchannels, JacsServiceData jacsServiceData) {
        return IntStream
                .range(0, nchannels)
                .mapToObj(channelNo -> getNiftiResizedGlobalAlignedSubjectChannelFile(args, channelNo, jacsServiceData))
                .collect(Collectors.toList());
    }

    private Path getNiftiResizedGlobalAlignedSubjectChannelFile(AlignmentArgs args, int channelNo, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + String.format("-RsRotGlobalAligned_rs_c%d.nii", channelNo));
    }

    private Path getResizedTargetFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.targetTemplate) + "_rs.v3draw");
    }

    private Path getNiftiResizedTargetFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.targetTemplate) + "_rs_c0.nii");
    }

    private Path getLocalSymmetricTransformFilePrefix(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-ccmi");
    }

    private Path getLocalSymmetricAffineTransformFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-ccmiAffine.txt");
    }

    private Path getLocalWarpFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-ccmiWarp.nii.gz");
    }

    private Path getLocalInverseWarpFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-ccmiInverseWarp.nii.gz");
    }

    private Path getNiftiResizedGlobalAlignedDeformedSubjectChannelFile(AlignmentArgs args, int channelNo, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + String.format("-RsRotGlobalAligned_rs_c%d_deformed.nii", channelNo));
    }

    private Path getResizedAlignedFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-AlignedSubjectRs.v3draw");
    }

    private Path getAlignedFile(AlignmentArgs args, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(args.input1File) + "-AlignedSubject.v3draw");
    }
}
