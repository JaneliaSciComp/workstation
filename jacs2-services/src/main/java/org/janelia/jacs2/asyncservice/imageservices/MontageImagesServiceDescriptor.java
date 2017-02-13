package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Create a square montage from PNGs in a given directory.
 */
@Named("montageImages")
public class MontageImagesServiceDescriptor implements ServiceDescriptor {

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

    private final MontageImagesProcessor montageImagesProcessor;

    @Inject
    MontageImagesServiceDescriptor(MontageImagesProcessor montageImagesProcessor) {
        this.montageImagesProcessor = montageImagesProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new MontageImagesArgs());
    }

    @Override
    public MontageImagesProcessor createServiceProcessor() {
        return montageImagesProcessor;
    }
}
