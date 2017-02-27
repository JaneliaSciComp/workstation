package org.janelia.jacs2.asyncservice;

import org.janelia.jacs2.asyncservice.common.ServiceProcessor;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

import java.util.List;

public interface ServiceRegistry {
    ServiceMetaData getServiceMetadata(String serviceName);
    List<ServiceMetaData> getAllServicesMetadata();
    ServiceProcessor<?> lookupService(String serviceName);
}
