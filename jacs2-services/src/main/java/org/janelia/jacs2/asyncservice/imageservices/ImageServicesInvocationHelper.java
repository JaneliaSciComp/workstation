package org.janelia.jacs2.asyncservice.imageservices;

import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.fileservices.LinkDataProcessor;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentConfiguration;
import org.janelia.jacs2.asyncservice.imageservices.align.ImageCoordinates;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class ImageServicesInvocationHelper {

    private final LinkDataProcessor linkDataProcessor;
    private final JacsServiceDataPersistence jacsServiceDataPersistence;
    private final Vaa3dConverterProcessor vaa3dConverterProcessor;
    private final Vaa3dPluginProcessor vaa3dPluginProcessor;
    private final NiftiConverterProcessor niftiConverterProcessor;
    private final WarpToolProcessor warpToolProcessor;
    private final Logger logger;

    ImageServicesInvocationHelper(JacsServiceDataPersistence jacsServiceDataPersistence,
                                  LinkDataProcessor linkDataProcessor,
                                  Vaa3dConverterProcessor vaa3dConverterProcessor,
                                  Vaa3dPluginProcessor vaa3dPluginProcessor,
                                  NiftiConverterProcessor niftiConverterProcessor,
                                  WarpToolProcessor warpToolProcessor,
                                  Logger logger) {
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
        this.linkDataProcessor = linkDataProcessor;
        this.vaa3dConverterProcessor = vaa3dConverterProcessor;
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
        this.niftiConverterProcessor = niftiConverterProcessor;
        this.warpToolProcessor = warpToolProcessor;
        this.logger = logger;
    }

    JacsServiceData applyIWarp2Transformation(Path subjectFile, Path transformationFile, Path outputFile,
                                              String otherParams,
                                              int dx, int dy, int dz,
                                              String description,
                                              JacsServiceData jacsServiceData, JacsServiceData... deps) {
        JacsServiceData estimateRotationsServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "iwarp2"),
                new ServiceArg("-output", outputFile.toString()),
                new ServiceArg("-pluginParams", String.format("#s %s", subjectFile)),
                new ServiceArg("-pluginParams", String.format("#a %s", transformationFile)),
                new ServiceArg("-pluginParams", String.format("#dx %d", dx)),
                new ServiceArg("-pluginParams", String.format("#dy %s", dy)),
                new ServiceArg("-pluginParams", String.format("#dz %s", dz)),
                new ServiceArg("-pluginParams", otherParams)
        );
        return submitNewServiceDependency(jacsServiceData, estimateRotationsServiceData);
    }

    JacsServiceData convertToNiftiImage(Path input, Path output,
                                        String description,
                                        JacsServiceData jacsServiceData, JacsServiceData... deps) {
        if (output.toFile().exists()) {
            return null;
        }
        JacsServiceData niftiConverterServiceData = niftiConverterProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-input", input.toString()),
                new ServiceArg("-output", output == null ? null : output.toString()) // generate the default output
        );
        return submitNewServiceDependency(jacsServiceData, niftiConverterServiceData);
    }

    JacsServiceData convertFile(Path input, Path output,
                                String description,
                                JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Convert {} => {}", input, output);
        JacsServiceData convertServiceData = vaa3dConverterProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .waitFor(deps)
                        .description(description)
                        .build(),
                new ServiceArg("-input", input.toString()),
                new ServiceArg("-output", output.toString())
        );
        return submitNewServiceDependency(jacsServiceData, convertServiceData);
    }

    JacsServiceData convertToNiftiImage(Path input, List<Path> outputs,
                                        String description,
                                        JacsServiceData jacsServiceData, JacsServiceData... deps) {
        if (!outputs.isEmpty() && outputs.stream().reduce(true, (b, p) -> b && p.toFile().exists(), (b1, b2) -> b1 && b2)) {
            return null;
        }
        JacsServiceData niftiConverterServiceData = niftiConverterProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-input", input.toString()),
                new ServiceArg("-output", outputs.stream().map(Path::toString).collect(Collectors.joining(",")))
        );
        return submitNewServiceDependency(jacsServiceData, niftiConverterServiceData);
    }

    JacsServiceData convertFromNiftiImage(List<Path> inputs, Path output, String otherParams,
                                          String description,
                                          JacsServiceData jacsServiceData, JacsServiceData... deps) {
        if (output.toFile().exists()) {
            return null;
        }
        JacsServiceData niftiConverterServiceData = niftiConverterProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-input", inputs.stream().map(Path::toString).collect(Collectors.joining(","))),
                new ServiceArg("-output", output.toString()),
                new ServiceArg("-pluginParams", otherParams)
        );
        return submitNewServiceDependency(jacsServiceData, niftiConverterServiceData);
    }


    JacsServiceData extractRefFromSubject(Path resizedSubjectFile, Path resizedSubjectRefFile, int refChannel,
                                          String description,
                                          JacsServiceData jacsServiceData, JacsServiceData... deps) {
        if (resizedSubjectRefFile.toFile().exists()) {
            return null;
        }
        JacsServiceData extractRefServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-plugin", "refExtract"),
                new ServiceArg("-pluginFunc", "refExtract"),
                new ServiceArg("-input", resizedSubjectFile.toString()),
                new ServiceArg("-output", resizedSubjectRefFile.toString()),
                new ServiceArg("-pluginParams", String.format("#c %d", refChannel))
        );
        return submitNewServiceDependency(jacsServiceData, extractRefServiceData);
    }

    JacsServiceData downsampleImage(Path input, Path output, double downsampleFactor,
                                    String description,
                                    JacsServiceData jacsServiceData, JacsServiceData... deps) {
        if (output.toFile().exists()) {
            return null;
        }
        JacsServiceData downsampleServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "resamplebyspacing"),
                new ServiceArg("-input", input.toString()),
                new ServiceArg("-output", output.toString()),
                new ServiceArg("-pluginParams", String.format("#x %f", downsampleFactor)),
                new ServiceArg("-pluginParams", String.format("#y %f", downsampleFactor)),
                new ServiceArg("-pluginParams", String.format("#z %f", downsampleFactor))
        );
        return submitNewServiceDependency(jacsServiceData, downsampleServiceData);
    }

    JacsServiceData isotropicSampling(Path sourceFile, AlignmentConfiguration alignConfig,
                                      ImageCoordinates imageResolution, Path isotropicFile,
                                      String otherParameters,
                                      String description,
                                      JacsServiceData jacsServiceData, JacsServiceData... deps) {
        if (isotropicFile.toFile().exists()) {
            return null;
        }
        double isx = imageResolution.x / alignConfig.misc.vSzIsX63x;
        double isy = imageResolution.y / alignConfig.misc.vSzIsY63x;
        double isz = imageResolution.z / alignConfig.misc.vSzIsZ63x;
        JacsServiceData isotropicSamplingServiceData;
        if (Math.abs(isx - 1.) >  0.01
                || Math.abs(isy - 1.) >  0.01
                || Math.abs(isz - 1.) >  0.01) {
            isotropicSamplingServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                            .description(description)
                            .waitFor(deps)
                            .build(),
                    new ServiceArg("-plugin", "ireg"),
                    new ServiceArg("-pluginFunc", "isampler"),
                    new ServiceArg("-input", sourceFile.toString()),
                    new ServiceArg("-output", isotropicFile.toString()),
                    new ServiceArg("-pluginParams", String.format("#x %f", isx)),
                    new ServiceArg("-pluginParams", String.format("#y %f", isy)),
                    new ServiceArg("-pluginParams", String.format("#z %f", isz)),
                    new ServiceArg("-pluginParams", otherParameters)
            );
        } else {
            isotropicSamplingServiceData = linkDataProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                            .description(description)
                            .waitFor(deps)
                            .build(),
                    new ServiceArg("-source", sourceFile.toString()),
                    new ServiceArg("-target", isotropicFile.toString())
            );
        }
        return submitNewServiceDependency(jacsServiceData, isotropicSamplingServiceData);
    }

    JacsServiceData linkData(Path sourceFile, Path targetFile, String description,
                             JacsServiceData jacsServiceData, JacsServiceData... deps) {
        JacsServiceData linkServiceData =
                linkDataProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                                .description(description)
                                .waitFor(deps)
                                .build(),
                        new ServiceArg("-source", sourceFile.toString()),
                        new ServiceArg("-target", targetFile.toString())
                );
        return submitNewServiceDependency(jacsServiceData, linkServiceData);
    }

    JacsServiceData resizeToTarget(Path sourceFile, Path targetFile, Path resizedFile,
                                   String otherParameters,
                                   String description,
                                   JacsServiceData jacsServiceData, JacsServiceData... deps) {
        if (resizedFile.toFile().exists()) {
            return null;
        }
        JacsServiceData resizeSubjectServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "resizeImage"),
                new ServiceArg("-output", resizedFile.toString()),
                new ServiceArg("-pluginParams", String.format("#s %s", sourceFile)),
                new ServiceArg("-pluginParams", String.format("#t %s", targetFile)),
                new ServiceArg("-pluginParams", otherParameters)
        );
        return submitNewServiceDependency(jacsServiceData, resizeSubjectServiceData);
    }

    JacsServiceData submitNewServiceDependency(JacsServiceData jacsServiceData, JacsServiceData dependency) {
        Optional<JacsServiceData> existingDependency = jacsServiceData.findSimilarDependency(dependency);
        if (existingDependency.isPresent()) {
            return existingDependency.get();
        } else {
            jacsServiceDataPersistence.saveHierarchy(dependency);
            return dependency;
        }
    }

    JacsServiceData warp(Path input, Path output, List<Path> references,
                         String interpolation,
                         String description, JacsServiceData jacsServiceData,
                         JacsServiceData... deps) {
        JacsServiceData warpServiceData = warpToolProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-dims", "3"),
                new ServiceArg("-i", input.toString()),
                new ServiceArg("-o", output.toString()),
                new ServiceArg("-r", references.stream().map(Path::toString).collect(Collectors.joining(","))),
                new ServiceArg(interpolation)
        );
        return submitNewServiceDependency(jacsServiceData, warpServiceData);
    }

}
