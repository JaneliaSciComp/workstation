package org.janelia.jacs2.sampleprocessing;

import org.janelia.it.jacs.model.domain.support.MongoMapping;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceDescriptor;
import org.janelia.jacs2.service.impl.ServiceComputation;

import javax.enterprise.inject.Instance;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import javax.inject.Named;

@Named("sage")
public class SageServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "sage";

    @Inject
    private Instance<SageComputation> sageComputationSource;

    @Override
    public ServiceMetaData getMetadata() {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        return smd;
    }

    @Override
    public ServiceComputation<Void> createComputationInstance() {
        return sageComputationSource.get();
    }

}
