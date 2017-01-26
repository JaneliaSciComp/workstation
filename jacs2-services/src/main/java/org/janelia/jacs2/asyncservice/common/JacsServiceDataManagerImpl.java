package org.janelia.jacs2.asyncservice.common;

import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.DataInterval;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.asyncservice.ServerStats;
import org.janelia.jacs2.asyncservice.JacsServiceDataManager;

import javax.inject.Inject;
import java.util.Date;

public class JacsServiceDataManagerImpl implements JacsServiceDataManager {

    private final JacsServiceDataPersistence jacsServiceDataPersistence;
    private final JacsServiceDispatcher jacsServiceDispatcher;

    @Inject
    JacsServiceDataManagerImpl(JacsServiceDataPersistence jacsServiceDataPersistence, JacsServiceDispatcher jacsServiceDispatcher) {
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
        this.jacsServiceDispatcher = jacsServiceDispatcher;
    }

    @Override
    public JacsServiceData retrieveServiceById(Long instanceId) {
        return jacsServiceDataPersistence.findById(instanceId);
    }

    @Override
    public PageResult<JacsServiceData> searchServices(JacsServiceData ref, DataInterval<Date> creationInterval, PageRequest pageRequest) {
        return jacsServiceDataPersistence.findMatchingServices(ref, creationInterval, pageRequest);
    }

    @Override
    public JacsServiceData submitServiceAsync(JacsServiceData serviceArgs) {
        return jacsServiceDispatcher.submitServiceAsync(serviceArgs);
    }

    @Override
    public JacsServiceData updateService(Long instanceId, JacsServiceData serviceData) {
        boolean updateEntireHierarchy = false;
        JacsServiceData existingService = jacsServiceDataPersistence.findServiceHierarchy(instanceId);
        if (existingService == null) {
            return null;
        }
        if (serviceData.getState() != null) {
            existingService.setState(serviceData.getState());
        }
        if (serviceData.getServiceTimeout() != null) {
            existingService.setServiceTimeout(serviceData.getServiceTimeout());
        }
        if (serviceData.getPriority() != null) {
            updateEntireHierarchy = true;
            existingService.updateServiceHierarchyPriority(serviceData.priority());
        }
        if (StringUtils.isNotBlank(serviceData.getWorkspace())) {
            existingService.setWorkspace(serviceData.getWorkspace());
        }
        if (updateEntireHierarchy) {
            jacsServiceDataPersistence.updateHierarchy(existingService);
        } else {
            jacsServiceDataPersistence.update(existingService);
        }
        return null;
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
