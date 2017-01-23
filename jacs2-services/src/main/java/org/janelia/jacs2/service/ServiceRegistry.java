package org.janelia.jacs2.service;

import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import java.util.List;

public interface ServiceRegistry {
    ServiceMetaData getServiceDescriptor(String serviceName);
    List<ServiceMetaData> getAllServiceDescriptors();
    ServiceDescriptor lookupService(String serviceName);
}
