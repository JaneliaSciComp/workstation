package org.janelia.jacs2.asyncservice.lsmfileservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("lsmFileMetadata")
public class LsmFileMetadataServiceDescriptor implements ServiceDescriptor {
    static class LsmFileMetadataArgs extends ServiceArgs {
        @Parameter(names = "-inputLSM", description = "LSM Input file name", required = true)
        String inputLSMFile;
        @Parameter(names = "-outputLSMMetadata", description = "Destination directory", required = true)
        String outputLSMMetadata;
    }

    private final LsmFileMetadataProcessor lsmMetadataProcessor;

    @Inject
    LsmFileMetadataServiceDescriptor(LsmFileMetadataProcessor lsmMetadataProcessor) {
        this.lsmMetadataProcessor = lsmMetadataProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new LsmFileMetadataArgs());
    }

    @Override
    public LsmFileMetadataProcessor createServiceProcessor() {
        return lsmMetadataProcessor;
    }

}
