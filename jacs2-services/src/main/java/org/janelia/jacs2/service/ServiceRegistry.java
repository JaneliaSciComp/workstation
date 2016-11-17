package org.janelia.jacs2.service;

import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

public interface ServiceRegistry {
    ServiceMetaData getServiceDescriptor(String serviceName);
    ServiceDescriptor lookupService(String serviceName);
}
