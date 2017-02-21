package org.janelia.jacs2.asyncservice.imageservices;

import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.asyncservice.utils.FileUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.slf4j.Logger;

import javax.inject.Inject;
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
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class BasicMIPsAndMoviesProcessor extends AbstractServiceProcessor<List<File>> {

    private static final String DEFAULT_OPTIONS = "mips:movies";

    private final String basicMIPsAndMoviesMacro;
    private final String scratchLocation;

    @Inject
    BasicMIPsAndMoviesProcessor(JacsServiceEngine jacsServiceEngine,
                                ServiceComputationFactory computationFactory,
                                JacsServiceDataPersistence jacsServiceDataPersistence,
                                @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                @PropertyValue(name = "Fiji.BasicMIPsAndMovies") String basicMIPsAndMoviesMacro,
                                @PropertyValue(name = "service.DefaultScratchDir") String scratchLocation,
                                Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.basicMIPsAndMoviesMacro = basicMIPsAndMoviesMacro;
        this.scratchLocation =scratchLocation;
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
    protected ServiceComputation<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        BasicMIPsAndMoviesServiceDescriptor.BasicMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        try {
            Files.createDirectories(getResultsDir(args));
        } catch (IOException e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return computationFactory.newCompletedComputation(jacsServiceData);
    }

    @Override
    protected ServiceComputation<List<File>> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        BasicMIPsAndMoviesServiceDescriptor.BasicMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        List<File> pngAndMpegFileResults = new ArrayList<>();
        return submitFijiService(args, jacsServiceData)
                .thenCompose(sd -> this.waitForCompletion(sd))
                .thenCompose(r -> this.collectResult(preProcessingResult, jacsServiceData))
                .thenCompose(fileResults -> {
                    fileResults
                            .stream()
                            .filter(f -> !f.getName().endsWith(".avi"))
                            .forEach(pngAndMpegFileResults::add);
                    List<ServiceComputation<?>> mpegConverters = fileResults.stream()
                            .filter(f -> f.getName().endsWith(".avi"))
                            .map(f -> {
                                JacsServiceData mpegConverterService =
                                        new JacsServiceDataBuilder(jacsServiceData)
                                                .setName("mpegConverter")
                                                .addArg("-input", f.getAbsolutePath())
                                                .build();
                                return createServiceComputation(jacsServiceEngine.submitSingleService(mpegConverterService))
                                        .thenCompose(sd -> this.waitForCompletion(sd));
                            })
                            .collect(Collectors.toList());
                    // collect the results by appending the generated MPEG to the list
                    return computationFactory.newCompletedComputation(fileResults)
                            .thenCombineAll(mpegConverters, (previousFileResults, mpegResults) -> {
                                List<File> finalResults = new ArrayList<>(previousFileResults);
                                mpegResults.forEach(mpegFile -> previousFileResults.add((File) mpegFile));
                                return finalResults;
                            });
                });
    }

    private Path getResultsDir(BasicMIPsAndMoviesServiceDescriptor.BasicMIPsAndMoviesArgs args) {
        return Paths.get(args.resultsDir, com.google.common.io.Files.getNameWithoutExtension(args.imageFile));
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        BasicMIPsAndMoviesServiceDescriptor.BasicMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        // collect all AVIs and PNGs
        try {
            String resultsPattern = "glob:**/*.{png,avi,mp4}";
            PathMatcher inputFileMatcher =
                    FileSystems.getDefault().getPathMatcher(resultsPattern);
            long nFiles = Files.find(getResultsDir(args), 1, (p, a) -> inputFileMatcher.matches(p)).count();
            return nFiles > 0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected List<File> retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        BasicMIPsAndMoviesServiceDescriptor.BasicMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        // collect all AVIs and PNGs
        List<File> results = new ArrayList<>();
        try {
            String resultsPattern = "glob:**/*.{png,avi,mp4}";
            PathMatcher inputFileMatcher =
                    FileSystems.getDefault().getPathMatcher(resultsPattern);
            Files.find(getResultsDir(args), 1, (p, a) -> inputFileMatcher.matches(p)).forEach(p -> results.add(p.toFile()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return results;
    }

    private ServiceComputation<JacsServiceData> submitFijiService(BasicMIPsAndMoviesServiceDescriptor.BasicMIPsAndMoviesArgs args, JacsServiceData jacsServiceData) {
        if (StringUtils.isBlank(args.chanSpec)) {
            throw new ComputationException(jacsServiceData,  "No channel spec for " + args.imageFile);
        }
        Path temporaryOutputDir = getServicePath(scratchLocation, jacsServiceData, com.google.common.io.Files.getNameWithoutExtension(args.imageFile));
        JacsServiceData fijiService =
                new JacsServiceDataBuilder(jacsServiceData)
                        .setName("fijiMacro")
                        .addArg("-macro", basicMIPsAndMoviesMacro)
                        .addArg("-macroArgs", getBasicMIPsAndMoviesArgs(args, temporaryOutputDir))
                        .addArg("-temporaryOutput", temporaryOutputDir.toString())
                        .addArg("-finalOutput", getResultsDir(args).toString())
                        .addArg("-resultsPatterns", "*.png")
                        .addArg("-resultsPatterns", "*.avi")
                        .build();
        return createServiceComputation(jacsServiceEngine.submitSingleService(fijiService));
    }

    private String getBasicMIPsAndMoviesArgs(BasicMIPsAndMoviesServiceDescriptor.BasicMIPsAndMoviesArgs args, Path outputDir) {
        List<FijiColor> colors = FijiUtils.getColorSpec(args.colorSpec, args.chanSpec);
        if (colors.isEmpty()) {
            colors = FijiUtils.getDefaultColorSpec(args.chanSpec, "RGB", '1');
        }
        String colorSpec = colors.stream().map(c -> String.valueOf(c.getCode())).collect(Collectors.joining(","));
        String divSpec = colors.stream().map(c -> String.valueOf(c.getDivisor())).collect(Collectors.joining(","));
        StringJoiner builder = new StringJoiner(",");
        builder.add(outputDir.toString()); // output directory
        builder.add(com.google.common.io.Files.getNameWithoutExtension(args.imageFile)); // output prefix 1
        builder.add(""); // output prefix 2
        builder.add(args.imageFile); // input file 1
        builder.add(""); // input file 2
        builder.add(args.laser == null ? "" : args.laser.toString());
        builder.add(args.gain == null ? "" : args.gain.toString());
        builder.add(args.chanSpec);
        builder.add(colorSpec);
        builder.add(divSpec);
        builder.add(StringUtils.defaultIfBlank(args.options, DEFAULT_OPTIONS));
        return builder.toString();
    }

    @Override
    protected ServiceComputation<List<File>> postProcessData(List<File> processingResult, JacsServiceData jacsServiceData) {
        return computationFactory.<List<File>>newComputation()
                .supply(() -> {
                    try {
                        Path temporaryOutputDir = getServicePath(scratchLocation, jacsServiceData);
                        FileUtils.deletePath(temporaryOutputDir);
                        return processingResult;
                    } catch (Exception e) {
                        throw new ComputationException(jacsServiceData, e);
                    }
                });
    }

    private BasicMIPsAndMoviesServiceDescriptor.BasicMIPsAndMoviesArgs getArgs(JacsServiceData jacsServiceData) {
        return BasicMIPsAndMoviesServiceDescriptor.BasicMIPsAndMoviesArgs.parse(jacsServiceData.getArgsArray(), new BasicMIPsAndMoviesServiceDescriptor.BasicMIPsAndMoviesArgs());
    }
}
