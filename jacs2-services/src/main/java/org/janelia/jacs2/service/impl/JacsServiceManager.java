package org.janelia.jacs2.service.impl;


import org.janelia.jacs2.dao.ServiceInfoDao;
import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.service.ServerStats;
import org.janelia.jacs2.service.ServiceManager;

import javax.inject.Inject;
import java.util.Optional;

public class JacsServiceManager implements ServiceManager {

    private final ServiceInfoDao serviceInfoDao;
    private final JacsServiceDispatcher serviceDispatcher;

    @Inject
    JacsServiceManager(ServiceInfoDao serviceInfoDao, JacsServiceDispatcher serviceDispatcher) {
        this.serviceInfoDao = serviceInfoDao;
        this.serviceDispatcher = serviceDispatcher;
    }

    @Override
    public ServiceInfo retrieveServiceInfo(Long instanceId) {
        return serviceInfoDao.findById(instanceId);
    }

    @Override
    public ServiceInfo startAsyncService(ServiceInfo serviceArgs, Optional<ServiceInfo> parentService) {
        serviceDispatcher.submitService(serviceArgs, parentService);
        return serviceArgs;
    }

    @Override
    public ServerStats getServerStats() {
        return serviceDispatcher.getServerStats();
    }
}
