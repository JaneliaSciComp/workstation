package org.janelia.jacs2.asyncservice.common;

import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.ServerStats;
import org.janelia.jacs2.asyncservice.ServiceRegistry;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.Semaphore;

@Singleton
public class JacsServiceEngineImpl implements JacsServiceEngine {
    private static final int DEFAULT_MAX_RUNNING_SLOTS = 1000;

    private final JacsServiceDataPersistence jacsServiceDataPersistence;
    private final JacsServiceQueue jacsServiceQueue;
    private final Instance<ServiceRegistry> serviceRegistrarSource;
    private final Logger logger;
    private int nAvailableSlots;
    private final Semaphore availableSlots;

    @Inject
    JacsServiceEngineImpl(JacsServiceDataPersistence jacsServiceDataPersistence,
                          JacsServiceQueue jacsServiceQueue,
                          Instance<ServiceRegistry> serviceRegistrarSource,
                          @PropertyValue(name = "service.engine.ProcessingSlots") int nAvailableSlots,
                          Logger logger) {
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
        this.jacsServiceQueue = jacsServiceQueue;
        this.serviceRegistrarSource = serviceRegistrarSource;
        this.logger = logger;
        this.nAvailableSlots = nAvailableSlots <= 0 ? DEFAULT_MAX_RUNNING_SLOTS : nAvailableSlots;
        availableSlots = new Semaphore(this.nAvailableSlots, true);
    }

    @Override
    public ServerStats getServerStats() {
        ServerStats stats = new ServerStats();
        stats.setWaitingCapacity(jacsServiceQueue.getMaxReadyCapacity());
        stats.setWaitingServices(jacsServiceQueue.getReadyServicesSize());
        stats.setAvailableSlots(getAvailableSlots());
        stats.setRunningServicesCount(jacsServiceQueue.getPendingServicesSize());
        stats.setRunningServices(jacsServiceQueue.getPendingServices());
        return stats;
    }

    private int getAvailableSlots() {
        return availableSlots.availablePermits();
    }

    @Override
    public void setProcessingSlotsCount(int nProcessingSlots) {
        int nDiff = nProcessingSlots - nAvailableSlots;
        if (nDiff > 0) {
            availableSlots.release(nDiff);
            nAvailableSlots = nProcessingSlots;
        } else if (nDiff < 0) {
            if (availableSlots.tryAcquire(-nDiff)) {
                nAvailableSlots = nProcessingSlots;
            }
        }
    }

    @Override
    public void setMaxWaitingSlots(int maxWaitingSlots) {
        jacsServiceQueue.setMaxReadyCapacity(maxWaitingSlots);
    }

    @Override
    public ServiceProcessor<?> getServiceProcessor(JacsServiceData jacsServiceData) {
        return getServiceProcessor(jacsServiceData.getName());
    }

    @Override
    public boolean acquireSlot() {
        return availableSlots.tryAcquire();
    }

    @Override
    public void releaseSlot() {
        availableSlots.release();
    }

    private ServiceProcessor<?> getServiceProcessor(String serviceName) {
        ServiceRegistry registrar = serviceRegistrarSource.get();
        ServiceProcessor<?> serviceProcessor = registrar.lookupService(serviceName);
        if (serviceProcessor == null) {
            logger.error("No service found for {}", serviceName);
            throw new IllegalArgumentException("Unknown service: " + serviceName);
        }
        return serviceProcessor;
    }

    @Override
    public JacsServiceData submitSingleService(JacsServiceData serviceArgs) {
        if (serviceArgs.hasParentServiceId()) {
            List<JacsServiceData> childServices = jacsServiceDataPersistence.findChildServices(serviceArgs.getParentServiceId());
            Optional<JacsServiceData> existingChildService =
                    childServices.stream()
                            .filter(s -> s.getName().equals(serviceArgs.getName()))
                            .filter(s -> s.getArgs().equals(serviceArgs.getArgs()))
                            .findFirst();
            if (existingChildService.isPresent()) {
                return existingChildService.get(); // do not resubmit
            }
        }
        jacsServiceDataPersistence.saveHierarchy(serviceArgs);
        return serviceArgs;
    }

    @Override
    public List<JacsServiceData> submitMultipleServices(List<JacsServiceData> listOfServices) {
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
            JacsServiceData submitted = submitSingleService(currentService);
            results.add(submitted);
            prevService = submitted;
        }
        return results;
    }

}
