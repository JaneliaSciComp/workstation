package org.janelia.jacs2.asyncservice.lsmfileservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("archivedLsmMetadata")
public class ArchivedLsmMetadataServiceDescriptor implements ServiceDescriptor {
    static class ArchivedLsmMetadataArgs extends ServiceArgs {
        @Parameter(names = "-archivedLSM", description = "Archived LSM file name", required = true)
        String archiveLSMFile;
        @Parameter(names = "-outputLSMMetadata", description = "Destination directory", required = true)
        String outputLSMMetadata;
        @Parameter(names = "-keepIntermediateLSM", arity = 0, description = "If used the temporary LSM file created from the archive will not be deleted", required = false)
        boolean keepIntermediateLSM = false;
    }

    private final ArchivedLsmMetadataProcessor archivedLsmMetadataProcessor;

    @Inject
    ArchivedLsmMetadataServiceDescriptor(ArchivedLsmMetadataProcessor archivedLsmMetadataProcessor) {
        this.archivedLsmMetadataProcessor = archivedLsmMetadataProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new ArchivedLsmMetadataArgs());
    }

    @Override
    public ArchivedLsmMetadataProcessor createServiceProcessor() {
        return archivedLsmMetadataProcessor;
    }

}
