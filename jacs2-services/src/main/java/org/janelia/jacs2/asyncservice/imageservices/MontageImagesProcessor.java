package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractSingleFileServiceResultHandler;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
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
import java.util.Map;

/**
 * Create a square montage from PNGs in a given directory.
 */
@Named("montageImages")
public class MontageImagesProcessor extends AbstractExeBasedServiceProcessor<Void, File> {

    static class MontageImagesArgs extends ServiceArgs {
        @Parameter(names = "-inputFolder", description = "Input folder", required = true)
        String inputFolder;
        @Parameter(names = "-size", description = "The size of the montage", required = true)
        int size;
        @Parameter(names = "-target", description = "Name of the target montage")
        String target;
        @Parameter(names = "-imageFilePattern", description = "The extension of the image files from the input folder")
        String imageFilePattern = "glob:**/*.png";
    }

    private final String montageToolLocation;
    private final String montageToolName;
    private final String libraryPath;

    @Inject
    MontageImagesProcessor(ServiceComputationFactory computationFactory,
                           JacsServiceDataPersistence jacsServiceDataPersistence,
                           @Any Instance<ExternalProcessRunner> serviceRunners,
                           @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                           @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                           @PropertyValue(name = "ImageMagick.Bin.Path") String montageToolLocation,
                           @PropertyValue(name = "ImageMagick.Montage.Name") String montageToolName,
                           @PropertyValue(name = "ImageMagick.Lib.Path") String libraryPath,
                           Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, serviceRunners, defaultWorkingDir, executablesBaseDir, logger);
        this.montageToolLocation = montageToolLocation;
        this.montageToolName = montageToolName;
        this.libraryPath = libraryPath;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new MontageImagesArgs());
    }

    @Override
    public ServiceResultHandler<File> getResultHandler() {
        return new AbstractSingleFileServiceResultHandler() {

            @Override
            public boolean isResultReady(JacsServiceResult<?> depResults) {
                MontageImagesArgs args = getArgs(depResults.getJacsServiceData());
                File targetImage = getTargetImage(args);
                return targetImage.exists();
            }

            @Override
            public File collectResult(JacsServiceResult<?> depResults) {
                MontageImagesArgs args = getArgs(depResults.getJacsServiceData());
                return getTargetImage(args);
            }
        };
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        MontageImagesArgs args = getArgs(jacsServiceData);
        Path inputPath = Paths.get(args.inputFolder);
        List<String> inputFiles = new ArrayList<>();
        try {
            PathMatcher inputFileMatcher =
                    FileSystems.getDefault().getPathMatcher(args.imageFilePattern);
            Files.find(inputPath, 1, (p, a) -> inputFileMatcher.matches(p)).forEach(p -> inputFiles.add(p.toString()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (inputFiles.isEmpty()) {
            throw new IllegalArgumentException("No image file found in " + args.inputFolder + " that matches the given pattern: " + args.imageFilePattern);
        }
        logger.info("Montage {}", inputFiles);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        externalScriptCode.getCodeWriter()
                .addWithArgs(getExecutable())
                .addArg("-background")
                .addArg("'#000000'")
                .addArg("-geometry")
                .addArg("'300x300>'")
                .addArg("-tile")
                .addArg(String.format("%dx%d", args.size, args.size))
                .addArgs(inputFiles)
                .endArgs(args.target);
        return externalScriptCode;
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, getFullExecutableName(libraryPath)));
    }

    private MontageImagesArgs getArgs(JacsServiceData jacsServiceData) {
        MontageImagesArgs args = new MontageImagesArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private File getTargetImage(MontageImagesArgs args) {
        return new File(args.target);
    }

    private String getExecutable() {
        return getFullExecutableName(montageToolLocation, montageToolName);
    }

}
