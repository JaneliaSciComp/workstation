package org.janelia.jacs2.fileservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

@Named("fileCopy")
public class FileCopyServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "fileCopy";

    static class FileCopyArgs {
        @Parameter(names = "-src", description = "Source file name", required = true)
        String sourceFilename;
        @Parameter(names = "-dst", description = "Destination file name or location", required = true)
        String targetFilename;
        @Parameter(names = "-mv", arity = 0, description = "If used the file will be moved to the target", required = false)
        boolean deleteSourceFile = false;
        @Parameter(names = "-convert8", arity = 0, description = "If set it converts the image to 8bit", required = false)
        boolean convertTo8Bits = false;
    }

    @Inject
    private Instance<FileCopyComputation> sageComputationSource;

    @Override
    public ServiceMetaData getMetadata() {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        return smd;
    }

    @Override
    public ServiceComputation createComputationInstance() {
        return sageComputationSource.get();
    }

}
