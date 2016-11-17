package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.model.service.ServiceMetaData;

public interface ServiceDescriptor {
    ServiceMetaData getMetadata();
    ServiceComputation createComputationInstance(ServiceInfo serviceInfo);
}
