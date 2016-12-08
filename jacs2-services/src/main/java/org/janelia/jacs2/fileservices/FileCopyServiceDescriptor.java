package org.janelia.jacs2.fileservices;

import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

@Named("fileCopy")
public class FileCopyServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "fileCopy";

    @Named("fileCopyService")
    @Inject
    private Instance<ServiceComputation> sageComputationSource;

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
