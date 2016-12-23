package org.janelia.jacs2.lsmfileservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

@Named("archivedLsmMetadata")
public class ArchivedLsmMetadataServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "archivedLsmMetadata";

    static class ArchivedLsmMetadataArgs {
        @Parameter(names = "-archivedLSM", description = "Archived LSM file name", required = true)
        String archiveLSMFile;
        @Parameter(names = "-outputLSMMetadata", description = "Destination directory", required = true)
        String outputLSMMetadata;
    }

    @Inject
    private Instance<ArchivedLsmMetadataComputation> lsmMetadataComputationSource;

    @Override
    public ServiceMetaData getMetadata() {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        return smd;
    }

    @Override
    public ServiceComputation createComputationInstance() {
        return lsmMetadataComputationSource.get();
    }

}
