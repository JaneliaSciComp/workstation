package org.janelia.jacs2.asyncservice.common;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.DataInterval;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.asyncservice.JacsServiceDataManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

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
    public JacsServiceData createSingleService(JacsServiceData serviceArgs) {
        return jacsServiceDispatcher.submitServiceAsync(serviceArgs);
    }

    @Override
    public List<JacsServiceData> createMultipleServices(List<JacsServiceData> listOfServices) {
        if (CollectionUtils.isEmpty(listOfServices)) {
            return listOfServices;
        }
        JacsServiceData prevService = null;
        List<JacsServiceData> results = new ArrayList<>();
        // update the service priorities so that the priorities descend for subsequent services
        int prevPriority = -1;
        for (ListIterator<JacsServiceData> servicesItr = listOfServices.listIterator(listOfServices.size()); servicesItr.hasPrevious();) {
            JacsServiceData currentService = servicesItr.previous();
            int currentPriority = currentService.priority();
            if (prevPriority >= 0) {
                int newPriority = prevPriority + 1;
                if (currentPriority < newPriority) {
                    currentPriority = newPriority;
                    currentService.updateServiceHierarchyPriority(currentPriority);
                }
            }
            prevPriority = currentPriority;
        }
        // submit the services and update their dependencies
        for (JacsServiceData currentService : listOfServices) {
            if (prevService != null) {
                currentService.addServiceDependency(prevService);
            }
            JacsServiceData submitted = jacsServiceDispatcher.submitServiceAsync(currentService);
            results.add(submitted);
            prevService = submitted;
        }
        return results;
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

}
