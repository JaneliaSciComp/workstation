package org.janelia.jacs2.asyncservice.common;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

@Singleton
public class InMemoryJacsServiceQueue implements JacsServiceQueue {
    private static final int DEFAULT_MAX_READY_CAPACITY = 20;

    private final JacsServiceDataPersistence jacsServiceDataPersistence;
    private final Queue<JacsServiceData> waitingServices;
    private final Set<Number> waitingServicesSet = new ConcurrentSkipListSet<>();
    private final Set<Number> submittedServicesSet = new ConcurrentSkipListSet<>();
    private final Semaphore queuePermit;
    private final Logger logger;
    private int maxReadyCapacity;
    private boolean noWaitingSpaceAvailable;

    @Inject
    public InMemoryJacsServiceQueue(JacsServiceDataPersistence jacsServiceDataPersistence,
                                    @PropertyValue(name = "service.queue.MaxCapacity") int maxReadyCapacity,
                                    Logger logger) {
        this.maxReadyCapacity = maxReadyCapacity == 0 ? DEFAULT_MAX_READY_CAPACITY : maxReadyCapacity;
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
        this.waitingServices = new PriorityBlockingQueue<>(this.maxReadyCapacity, new DefaultServiceInfoComparator());
        this.queuePermit = new Semaphore(1, true);
        this.logger = logger;
        noWaitingSpaceAvailable = false;
    }

    @Override
    public JacsServiceData enqueueService(JacsServiceData jacsServiceData) {
        persistServiceInfo(jacsServiceData);
        if (noWaitingSpaceAvailable) {
            // don't even check if anything has become available since last time
            // just drop it for now - the queue will be refilled after it drains.
            logger.info("In memory queue reached the capacity so service {} will not be put in memory", jacsServiceData);
            return jacsServiceData;
        }
        boolean added = addWaitingService(jacsServiceData);
        noWaitingSpaceAvailable  = !added || (waitingCapacity() <= 0);
        if (noWaitingSpaceAvailable) {
            logger.info("Not enough space in memory queue for {}", jacsServiceData);
        }
        return jacsServiceData;
    }

    @Override
    public JacsServiceData dequeService() {
        JacsServiceData queuedService = getWaitingService();
        if (queuedService == null && enqueueAvailableServices(EnumSet.of(JacsServiceState.CREATED, JacsServiceState.QUEUED))) {
            queuedService = getWaitingService();
        }
        return queuedService;
    }

    @Override
    public void refreshServiceQueue() {
        logger.debug("Sync the waiting queue");
        // check for newly created services and queue them based on their priorities
        enqueueAvailableServices(EnumSet.of(JacsServiceState.CREATED));
    }

    @Override
    public void completeService(JacsServiceData jacsServiceData) {
        submittedServicesSet.remove(jacsServiceData.getId());
    }

    @Override
    public int getReadyServicesSize() {
        return waitingServices.size();
    }

    @Override
    public int getPendingServicesSize() {
        return submittedServicesSet.size();
    }

    @Override
    public List<Number> getPendingServices() {
        return ImmutableList.copyOf(submittedServicesSet);
    }

    @Override
    public int getMaxReadyCapacity() {
        return maxReadyCapacity;
    }

    @Override
    public void setMaxReadyCapacity(int maxReadyCapacity) {
        this.maxReadyCapacity = maxReadyCapacity <= 0 ? DEFAULT_MAX_READY_CAPACITY : maxReadyCapacity;
    }

    private void persistServiceInfo(JacsServiceData jacsServiceData) {
        jacsServiceData.setState(JacsServiceState.CREATED);
        jacsServiceDataPersistence.saveHierarchy(jacsServiceData);
    }

    private boolean addWaitingService(JacsServiceData jacsServiceData) {
        boolean added = false;
        try {
            queuePermit.acquireUninterruptibly();
            if (submittedServicesSet.contains(jacsServiceData.getId()) || waitingServicesSet.contains(jacsServiceData.getId())) {
                // service is already waiting or running
                return true;
            }
            added = waitingServices.offer(jacsServiceData);
            if (added) {
                logger.debug("Enqueued service {}", jacsServiceData);
                waitingServicesSet.add(jacsServiceData.getId());
                if (jacsServiceData.getState() == JacsServiceState.CREATED) {
                    jacsServiceData.setState(JacsServiceState.QUEUED);
                    jacsServiceDataPersistence.update(jacsServiceData);
                }
            }
        } finally {
            queuePermit.release();
        }
        return added;
    }

    private boolean enqueueAvailableServices(Set<JacsServiceState> jacsServiceStates) {
        int availableSpaces = maxReadyCapacity;
        PageRequest servicePageRequest = new PageRequest();
        servicePageRequest.setPageSize(availableSpaces);
        servicePageRequest.setSortCriteria(new ArrayList<>(ImmutableList.of(
                new SortCriteria("priority", SortDirection.DESC),
                new SortCriteria("creationDate"))));
        PageResult<JacsServiceData> services = jacsServiceDataPersistence.findServicesByState(jacsServiceStates, servicePageRequest);
        if (CollectionUtils.isNotEmpty(services.getResultList())) {
            services.getResultList().stream().forEach(serviceData -> {
                try {
                    Preconditions.checkArgument(serviceData.getId() != null, "Invalid service ID");
                    if (!submittedServicesSet.contains(serviceData.getId()) && !waitingServicesSet.contains(serviceData.getId())) {
                        addWaitingService(serviceData);
                    }
                } catch (Exception e) {
                    logger.error("Internal error - no computation can be created for {}", serviceData);
                }
            });
            noWaitingSpaceAvailable = waitingCapacity() <= 0;
            return true;
        }
        return false;
    }

    private JacsServiceData getWaitingService() {
        try {
            queuePermit.acquireUninterruptibly();
            JacsServiceData jacsServiceData = waitingServices.poll();
            if (jacsServiceData != null) {
                logger.debug("Retrieved waiting service {}", jacsServiceData);
                Number serviceId = jacsServiceData.getId();
                submittedServicesSet.add(serviceId);
                waitingServicesSet.remove(serviceId);
            }
            return jacsServiceData;
        } finally {
            queuePermit.release();
        }
    }

    private int waitingCapacity() {
        int remainingCapacity = maxReadyCapacity - waitingServices.size();
        return remainingCapacity < 0 ? 0 : remainingCapacity;
    }

}
