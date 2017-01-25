package org.janelia.jacs2.asyncservice;

import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import java.util.List;

public interface ServiceRegistry {
    ServiceMetaData getServiceDescriptor(String serviceName);
    List<ServiceMetaData> getAllServiceDescriptors();
    ServiceDescriptor lookupService(String serviceName);
}
