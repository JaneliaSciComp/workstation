package org.janelia.jacs2.asyncservice.fileservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("fileCopy")
public class FileCopyServiceDescriptor implements ServiceDescriptor {
    public static class FileCopyArgs extends ServiceArgs {
        @Parameter(names = "-src", description = "Source file name", required = true)
        String sourceFilename;
        @Parameter(names = "-dst", description = "Destination file name or location", required = true)
        String targetFilename;
        @Parameter(names = "-mv", arity = 0, description = "If used the file will be moved to the target", required = false)
        boolean deleteSourceFile = false;
        @Parameter(names = "-convert8", arity = 0, description = "If set it converts the image to 8bit", required = false)
        boolean convertTo8Bits = false;
    }

    private final FileCopyProcessor processor;

    @Inject
    FileCopyServiceDescriptor(FileCopyProcessor processor) {
        this.processor = processor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new FileCopyArgs());
    }

    @Override
    public FileCopyProcessor createServiceProcessor() {
        return processor;
    }
}
