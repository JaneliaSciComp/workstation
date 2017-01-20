package org.janelia.jacs2.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Create a square montage from PNGs in a given directory.
 */
@Named("montageImages")
public class MontageImagesServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "montageImages";

    static class MontageImagesArgs {
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
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        MontageImagesArgs args = new MontageImagesArgs();
        StringBuilder usageOutput = new StringBuilder();
        JCommander jc = new JCommander(args);
        jc.setProgramName(SERVICE_NAME);
        jc.usage(usageOutput);
        smd.setUsage(usageOutput.toString());
        return smd;
    }

    @Override
    public MontageImagesProcessor createServiceProcessor() {
        return montageImagesProcessor;
    }
}
