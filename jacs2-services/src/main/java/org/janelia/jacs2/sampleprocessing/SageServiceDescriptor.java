package org.janelia.jacs2.sampleprocessing;

import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.model.service.ServiceState;
import org.janelia.jacs2.service.impl.ServiceDescriptor;
import org.janelia.jacs2.service.impl.ServiceComputation;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

@Named("sage")
public class SageServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "sage";

    @Named("sageService")
    @Inject
    private Instance<ServiceComputation> sageComputationSource;

    @Override
    public ServiceMetaData getMetadata() {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        return smd;
    }

    @Override
    public ServiceComputation createComputationInstance(ServiceInfo serviceInfo) {
        serviceInfo.setName(SERVICE_NAME);
        serviceInfo.setServiceType(SageComputation.class.getName());
        serviceInfo.setState(ServiceState.CREATED);
        ServiceComputation sageComputation = sageComputationSource.get();
        sageComputation.setComputationInfo(serviceInfo);
        return sageComputation;
    }

}
