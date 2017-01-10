package org.janelia.jacs2.imageservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.service.impl.AbstractExternalProcessComputation;
import org.janelia.jacs2.service.impl.JacsService;
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
import java.util.Map;

@Named("montageImagesService")
public class MontageImagesComputation extends AbstractExternalProcessComputation<File> {

    private final String montageToolLocation;
    private final String montageToolName;
    private final String libraryPath;
    private final Logger logger;

    @Inject
    public MontageImagesComputation(@PropertyValue(name = "ImageMagick.Bin.Path") String montageToolLocation,
                                    @PropertyValue(name = "ImageMagick.Montage.Name") String montageToolName,
                                    @PropertyValue(name = "ImageMagick.Lib.Path") String libraryPath,
                                    Logger logger) {
        this.montageToolLocation = montageToolLocation;
        this.montageToolName = montageToolName;
        this.libraryPath = libraryPath;
        this.logger = logger;
    }

    @Override
    protected List<String> prepareCmdArgs(JacsService<File> jacsService) {
        MontageImagesServiceDescriptor.MontageImagesArgs args = getArgs(jacsService);
        jacsService.setServiceCmd(getExecutable());
        Path inputPath = Paths.get(args.inputFolder);
        List<String> inputFiles = new ArrayList<>();
        try {
            PathMatcher inputFileMatcher =
                    FileSystems.getDefault().getPathMatcher(args.imageFilePattern);
            Files.find(inputPath, 1, (p, a) -> inputFileMatcher.matches(p)).forEach(p -> {
                inputFiles.add(p.toString());
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (inputFiles.isEmpty()) {
            throw new IllegalArgumentException("No image file found in " + args.inputFolder + " that matches the given pattern: " + args.imageFilePattern);
        }
        logger.info("Montage {}", inputFiles);
        return new ImmutableList.Builder<String>()
                .add("-background")
                .add("'#000000'")
                .add("-geometry")
                .add("'300x300>'")
                .add("-tile")
                .add(String.format("%dx%d", args.size, args.size))
                .addAll(inputFiles)
                .build();
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsService<File> jacsService) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    private MontageImagesServiceDescriptor.MontageImagesArgs getArgs(JacsService<File> jacsService) {
        MontageImagesServiceDescriptor.MontageImagesArgs args = new MontageImagesServiceDescriptor.MontageImagesArgs();
        new JCommander(args).parse(jacsService.getArgsArray());
        return args;
    }

    private String getExecutable() {
        return getFullExecutableName(montageToolLocation, montageToolName);
    }

}
