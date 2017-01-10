package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

@Named("sampleImageFiles")
public class GetSampleImageFilesServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "sampleImageFiles";

    static class SampleImageFilesArgs {
        @Parameter(names = "-sampleId", description = "Sample ID", required = true)
        Long sampleId;
        @Parameter(names = "-objective",
                   description = "Optional sample objective. If specified it retrieves all sample image files, otherwise it only retrieves the ones for the given objective", required = false)
        String sampleObjective;
        @Parameter(names = "-dest", description = "Destination directory or URL", required = true)
        String destFolder;
        @Parameter(names = "-keepIntermediateLSMFiles", arity = 0, description = "If used the temporary LSM files created from the archive will not be deleted", required = false)
        boolean keepIntermediateLSMFiles = false;
    }

    @Inject
    private Instance<CreateSampleLsmMetadataComputation> sampleLsmMetadataComputationSource;

    @Override
    public ServiceMetaData getMetadata() {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        SampleImageFilesArgs args = new SampleImageFilesArgs();
        StringBuilder usageOutput = new StringBuilder();
        JCommander jc = new JCommander(args);
        jc.setProgramName(SERVICE_NAME);
        jc.usage(usageOutput);
        smd.setUsage(usageOutput.toString());
        return smd;
    }

    @Override
    public ServiceComputation createComputationInstance() {
        return sampleLsmMetadataComputationSource.get();
    }

}
