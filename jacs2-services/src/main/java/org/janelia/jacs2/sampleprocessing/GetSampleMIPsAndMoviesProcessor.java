package org.janelia.jacs2.sampleprocessing;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.imageservices.FijiColor;
import org.janelia.jacs2.imageservices.FijiUtils;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.service.impl.AbstractServiceProcessor;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.JacsServiceDispatcher;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceComputationFactory;
import org.janelia.jacs2.service.impl.ServiceDataUtils;
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

public class GetSampleMIPsAndMoviesProcessor extends AbstractServiceProcessor<List<File>> {

    private static final String DEFAULT_OPTIONS = "mips:movies";

    private final String basicMIPsAndMoviesMacro;

    @Inject
    GetSampleMIPsAndMoviesProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                                    ServiceComputationFactory computationFactory,
                                    JacsServiceDataPersistence jacsServiceDataPersistence,
                                    @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                    @PropertyValue(name = "Fiji.BasicMIPsAndMovies") String basicMIPsAndMoviesMacro,
                                    Logger logger) {
        super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.basicMIPsAndMoviesMacro = basicMIPsAndMoviesMacro;
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
    protected ServiceComputation<List<SampleImageFile>> preProcessData(JacsServiceData jacsServiceData) {
        JacsServiceData sampleLSMsServiceData = SampleServicesUtils.createChildSampleServiceData("getSampleImageFiles", getArgs(jacsServiceData), jacsServiceData);
        return submitChildService(jacsServiceData, sampleLSMsServiceData)
                .thenCompose(sd -> this.waitForCompletion(sd))
                .thenApply(r -> ServiceDataUtils.stringToAny(sampleLSMsServiceData.getStringifiedResult(), new TypeReference<List<SampleImageFile>>() {
                }));
    }

    @Override
    protected ServiceComputation<List<File>> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        List<SampleImageFile> sampleLSMs = (List<SampleImageFile>) preProcessingResult;
        GetSampleMIPsAndMoviesServiceDescriptor.SampleMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        Path outputDir = Paths.get(args.sampleDataDir, args.mipsSubDir);
        try {
            // the output directory must exist
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new ComputationException(jacsServiceData, e);
        }
        List<ServiceComputation<?>> mipsComputations = submitAllFijiServices(sampleLSMs, args, jacsServiceData, outputDir);
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenCombineAll(mipsComputations, (sd, sampleMIPsResults) -> sampleMIPsResults)
                .thenCompose(r -> this.collectResult(preProcessingResult, jacsServiceData));
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return jacsServiceData.hasCompletedSuccessfully();
    }

    @Override
    protected List<File> retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        GetSampleMIPsAndMoviesServiceDescriptor.SampleMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        Path outputDir = Paths.get(args.sampleDataDir, args.mipsSubDir);
        // collect all AVIs and PNGs
        String resultsPattern = "glob:**/*.{png,avi,mp4}";
        List<File> results = new ArrayList<>();
        try {
            PathMatcher inputFileMatcher =
                    FileSystems.getDefault().getPathMatcher(resultsPattern);
            java.nio.file.Files.find(outputDir, 1, (p, a) -> inputFileMatcher.matches(p)).forEach(p -> results.add(p.toFile()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return results;
    }

    private List<ServiceComputation<?>> submitAllFijiServices(List<SampleImageFile> lsmFiles, GetSampleMIPsAndMoviesServiceDescriptor.SampleMIPsAndMoviesArgs args, JacsServiceData jacsServiceData, Path outputDir) {
        List<ServiceComputation<?>> fijiComputations = new ArrayList<>();
        lsmFiles.forEach(f -> {
            if (!f.isChanSpecDefined()) {
                throw new ComputationException(jacsServiceData,
                        "No channel spec for LSM " + f.getId() + "-" + f.getArchiveFilePath());
            }
            JacsServiceData fijiService =
                    new JacsServiceDataBuilder(jacsServiceData)
                            .setName("fijiMacro")
                            .addArg("-macro", basicMIPsAndMoviesMacro)
                            .addArg("-macroArgs", getBasicMIPsAndMoviesArgs(f, args, outputDir))
                            .build();
            fijiComputations.add(
                    this.submitChildService(jacsServiceData, fijiService)
                            .thenCompose(sd -> this.waitForCompletion(sd))

            );
        });
        return fijiComputations;
    }

    private String getBasicMIPsAndMoviesArgs(SampleImageFile sampleImageFile, GetSampleMIPsAndMoviesServiceDescriptor.SampleMIPsAndMoviesArgs args, Path outputDir) {
        List<FijiColor> colors = FijiUtils.getColorSpec(sampleImageFile.getColorSpec(), sampleImageFile.getChanSpec());
        if (colors.isEmpty()) {
            colors = FijiUtils.getDefaultColorSpec(sampleImageFile.getChanSpec(), "RGB", '1');
        }
        String colorSpec = colors.stream().map(c -> String.valueOf(c.getCode())).collect(Collectors.joining(","));
        String divSpec = colors.stream().map(c -> String.valueOf(c.getDivisor())).collect(Collectors.joining(","));
        StringJoiner builder = new StringJoiner(",");
        builder.add(outputDir.toString()); // output directory
        builder.add(com.google.common.io.Files.getNameWithoutExtension(sampleImageFile.getWorkingFilePath())); // output prefix 1
        builder.add(""); // output prefix 2
        builder.add(sampleImageFile.getWorkingFilePath()); // input file 1
        builder.add(""); // input file 2
        builder.add(sampleImageFile.getLaser() == null ? "" : sampleImageFile.getLaser().toString());
        builder.add(sampleImageFile.getGain() == null ? "" : sampleImageFile.getGain().toString());
        builder.add(StringUtils.defaultString(sampleImageFile.getChanSpec(), ""));
        builder.add(colorSpec);
        builder.add(divSpec);
        builder.add(StringUtils.defaultIfBlank(args.options, DEFAULT_OPTIONS));
        return builder.toString();
    }

    private GetSampleMIPsAndMoviesServiceDescriptor.SampleMIPsAndMoviesArgs getArgs(JacsServiceData jacsServiceData) {
        return SampleServiceArgs.parse(jacsServiceData.getArgsArray(), new GetSampleMIPsAndMoviesServiceDescriptor.SampleMIPsAndMoviesArgs());
    }
}
