package org.janelia.jacs2.service;

import org.janelia.jacs2.model.service.ServiceInfo;

import java.util.List;
import java.util.Optional;

public interface ServiceManager {
    ServiceInfo retrieveServiceInfo(Long instanceId);
    ServiceInfo startAsyncService(ServiceInfo serviceArgs, Optional<ServiceInfo> parentService);
    ServerStats getServerStats();
}
