package org.janelia.jacs2.asyncservice.common;

import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.DataInterval;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.asyncservice.JacsServiceDataManager;

import javax.inject.Inject;
import java.util.Date;

public class JacsServiceDataManagerImpl implements JacsServiceDataManager {

    private final JacsServiceDataPersistence jacsServiceDataPersistence;

    @Inject
    JacsServiceDataManagerImpl(JacsServiceDataPersistence jacsServiceDataPersistence) {
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
    }

    @Override
    public JacsServiceData retrieveServiceById(Number instanceId) {
        return jacsServiceDataPersistence.findById(instanceId);
    }

    @Override
    public PageResult<JacsServiceData> searchServices(JacsServiceData ref, DataInterval<Date> creationInterval, PageRequest pageRequest) {
        return jacsServiceDataPersistence.findMatchingServices(ref, creationInterval, pageRequest);
    }

    @Override
    public JacsServiceData updateService(Number instanceId, JacsServiceData serviceData) {
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
        return existingService;
    }

}
