package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Named("basicMIPsAndMovies")
public class BasicMIPsAndMoviesProcessor extends AbstractServiceProcessor<List<File>> {

    static class BasicMIPsAndMoviesArgs extends ServiceArgs {
        @Parameter(names = "-imgFile", description = "The name of the image file", required = true)
        String imageFile;
        @Parameter(names = "-chanSpec", description = "Channel spec", required = true)
        String chanSpec;
        @Parameter(names = "-colorSpec", description = "Color spec", required = false)
        String colorSpec;
        @Parameter(names = "-laser", description = "Laser", required = false)
        Integer laser;
        @Parameter(names = "-gain", description = "Gain", required = false)
        Integer gain;
        @Parameter(names = "-resultsDir", description = "Results directory", required = false)
        public String resultsDir;
        @Parameter(names = "-options", description = "Options", required = false)
        String options = "mips:movies:legends:bcomp";
    }

    private static final String DEFAULT_OPTIONS = "mips:movies";

    private final String basicMIPsAndMoviesMacro;
    private final String scratchLocation;
    private final FijiMacroProcessor fijiMacroProcessor;
    private final VideoFormatConverterProcessor mpegConverterProcessor;

    @Inject
    BasicMIPsAndMoviesProcessor(JacsServiceEngine jacsServiceEngine,
                                ServiceComputationFactory computationFactory,
                                JacsServiceDataPersistence jacsServiceDataPersistence,
                                @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                @PropertyValue(name = "Fiji.BasicMIPsAndMovies") String basicMIPsAndMoviesMacro,
                                @PropertyValue(name = "service.DefaultScratchDir") String scratchLocation,
                                Logger logger,
                                FijiMacroProcessor fijiMacroProcessor,
                                VideoFormatConverterProcessor mpegConverterProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.basicMIPsAndMoviesMacro = basicMIPsAndMoviesMacro;
        this.scratchLocation =scratchLocation;
        this.fijiMacroProcessor = fijiMacroProcessor;
        this.mpegConverterProcessor = mpegConverterProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new BasicMIPsAndMoviesArgs());
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
        BasicMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        try {
            Files.createDirectories(getResultsDir(args));
        } catch (IOException e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return createComputation(jacsServiceData);
    }

    @Override
    protected List<JacsServiceData> submitAllDependencies(JacsServiceData jacsServiceData) {
        BasicMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        return ImmutableList.of(submitFijiService(args, jacsServiceData));
    }

    @Override
    protected ServiceComputation<JacsServiceData> processData(JacsServiceData jacsServiceData) {
        return createComputation(jacsServiceData)
                .thenCompose(this::waitForDependencies)
                .thenApply(this::retrieveResultWhenReady)
                .thenApply(fileResults -> {
                    fileResults.stream()
                            .filter(f -> f.getName().endsWith(".avi"))
                            .forEach(f -> mpegConverterProcessor.create(new ServiceExecutionContext(jacsServiceData), new ServiceArg("-input", f.getAbsolutePath())));
                    return jacsServiceData;
                })
                .thenCompose(this::waitForDependencies);
    }

    private Path getResultsDir(BasicMIPsAndMoviesArgs args) {
        return Paths.get(args.resultsDir, com.google.common.io.Files.getNameWithoutExtension(args.imageFile));
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        BasicMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        // count AVIs, PNGs and MP4s
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
    protected List<File> retrieveResult(JacsServiceData jacsServiceData) {
        BasicMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        // collect all AVIs, PNGs and MP4s
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

    private JacsServiceData submitFijiService(BasicMIPsAndMoviesArgs args, JacsServiceData jacsServiceData) {
        if (StringUtils.isBlank(args.chanSpec)) {
            throw new ComputationException(jacsServiceData,  "No channel spec for " + args.imageFile);
        }
        Path temporaryOutputDir = getServicePath(scratchLocation, jacsServiceData, com.google.common.io.Files.getNameWithoutExtension(args.imageFile));
        return fijiMacroProcessor.create(new ServiceExecutionContext(jacsServiceData),
                new ServiceArg("-macro", basicMIPsAndMoviesMacro),
                new ServiceArg("-macroArgs", getBasicMIPsAndMoviesArgs(args, temporaryOutputDir)),
                new ServiceArg("-temporaryOutput", temporaryOutputDir.toString()),
                new ServiceArg("-finalOutput", getResultsDir(args).toString()),
                new ServiceArg("-resultsPatterns", "*.png"),
                new ServiceArg("-resultsPatterns", "*.avi"));
    }

    private String getBasicMIPsAndMoviesArgs(BasicMIPsAndMoviesArgs args, Path outputDir) {
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
    protected ServiceComputation<JacsServiceData> postProcessData(JacsServiceData jacsServiceData) {
        return computationFactory.<JacsServiceData>newComputation()
                .supply(() -> {
                    try {
                        Path temporaryOutputDir = getServicePath(scratchLocation, jacsServiceData);
                        FileUtils.deletePath(temporaryOutputDir);
                        return jacsServiceData;
                    } catch (Exception e) {
                        throw new ComputationException(jacsServiceData, e);
                    }
                });
    }

    private BasicMIPsAndMoviesArgs getArgs(JacsServiceData jacsServiceData) {
        return BasicMIPsAndMoviesArgs.parse(jacsServiceData.getArgsArray(), new BasicMIPsAndMoviesArgs());
    }
}
