package org.janelia.jacs2.service.impl;


import org.janelia.jacs2.dao.JacsServiceDataDao;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.ServerStats;
import org.janelia.jacs2.service.JacsServiceDataManager;

import javax.inject.Inject;
import java.util.Optional;

public class JacsServiceDataManagerImpl implements JacsServiceDataManager {

    private final JacsServiceDataDao jacsServiceDataDao;
    private final JacsServiceDispatcher jacsServiceDispatcher;

    @Inject
    JacsServiceDataManagerImpl(JacsServiceDataDao jacsServiceDataDao, JacsServiceDispatcher jacsServiceDispatcher) {
        this.jacsServiceDataDao = jacsServiceDataDao;
        this.jacsServiceDispatcher = jacsServiceDispatcher;
    }

    @Override
    public JacsServiceData retrieveServiceData(Long instanceId) {
        return jacsServiceDataDao.findById(instanceId);
    }

    @Override
    public JacsServiceData submitServiceAsync(JacsServiceData serviceArgs, Optional<JacsServiceData> parentService) {
        return jacsServiceDispatcher.submitServiceAsync(serviceArgs, parentService);
    }

    @Override
    public ServerStats getServerStats() {
        return jacsServiceDispatcher.getServerStats();
    }

    @Override
    public void setProcessingSlotsCount(int nProcessingSlots) {
        jacsServiceDispatcher.setAvailableSlots(nProcessingSlots);
    }
}
