package org.janelia.jacs2.sampleprocessing;

import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.model.service.ServiceState;
import org.janelia.jacs2.service.impl.ServiceDescriptor;
import org.janelia.jacs2.service.impl.ServiceComputation;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

@Named("neuronSeparator")
public class NeuronSeparatorServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "neuronSeparator";

    @Named("neuronSeparatorService")
    @Inject
    private Instance<ServiceComputation> neuronSeparatorComputationSource;

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
        ServiceComputation neuronSeparatorComputation = neuronSeparatorComputationSource.get();
        neuronSeparatorComputation.setComputationInfo(serviceInfo);
        return neuronSeparatorComputation;
    }

}
