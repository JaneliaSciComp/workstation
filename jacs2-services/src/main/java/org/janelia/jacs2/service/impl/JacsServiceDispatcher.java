package org.janelia.jacs2.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceState;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.service.ServerStats;
import org.janelia.jacs2.service.ServiceRegistry;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

@Singleton
public class JacsServiceDispatcher {

    private static final int MAX_WAITING_SLOTS = 20;
    private static final int MAX_RUNNING_SLOTS = 1000;

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private ExecutorService serviceExecutor;
    @Inject
    private JacsServiceDataPersistence jacsServiceDataPersistence;
    @Inject
    private Instance<ServiceRegistry> serviceRegistrarSource;
    private final Queue<JacsServiceData> waitingServices;
    private final Set<Long> waitingServicesSet = new ConcurrentSkipListSet<>();
    private final Set<Long> submittedServicesSet = new ConcurrentSkipListSet<>();
    private final Semaphore queuePermit;
    private int nAvailableSlots;
    private final Semaphore availableSlots;
    private boolean noWaitingSpaceAvailable;

    public JacsServiceDispatcher() {
        queuePermit = new Semaphore(1, true);
        nAvailableSlots = MAX_RUNNING_SLOTS;
        availableSlots = new Semaphore(nAvailableSlots, true);
        waitingServices = new PriorityBlockingQueue<>(MAX_WAITING_SLOTS, new DefaultServiceInfoComparator());
        noWaitingSpaceAvailable = false;
    }

    public void setAvailableSlots(int numberOfSlots) {
        int nDiff = numberOfSlots - nAvailableSlots;
        if (nDiff > 0) {
            availableSlots.release(nDiff);
            nAvailableSlots = numberOfSlots;
        } else if (nDiff < 0) {
            if (availableSlots.tryAcquire(-nDiff)) {
                nAvailableSlots = numberOfSlots;
            }
        }
    }

    public JacsServiceData submitServiceAsync(JacsServiceData serviceData, Optional<JacsServiceData> optionalParentService) {
        logger.info("Submitted {} as a {}", serviceData, optionalParentService.isPresent() ? "child of " + optionalParentService.get().getId() : "root service");
        if (optionalParentService.isPresent()) {
            serviceData.updateParentService(optionalParentService.get());
        }
        persistServiceInfo(serviceData);
        enqueueService(serviceData);
        return serviceData;
    }

    ServiceComputation<?> getServiceComputation(JacsServiceData jacsServiceData) {
        ServiceDescriptor serviceDescriptor = getServiceDescriptor(jacsServiceData.getName());
        return serviceDescriptor.createComputationInstance();
    }

    private ServiceDescriptor getServiceDescriptor(String serviceName) {
        ServiceRegistry registrar = serviceRegistrarSource.get();
        ServiceDescriptor serviceDescriptor = registrar.lookupService(serviceName);
        if (serviceDescriptor == null) {
            logger.error("No service found for {}", serviceName);
            throw new IllegalArgumentException("Unknown service: " + serviceName);
        }
        return serviceDescriptor;
    }

    private void enqueueService(JacsServiceData queuedService) {
        if (noWaitingSpaceAvailable) {
            // don't even check if anything has become available since last time
            // just drop it for now - the queue will be refilled after it drains.
            logger.info("In memory queue reached the capacity so service {} will not be put in memory", queuedService);
            return;
        }
        boolean added = addWaitingService(queuedService);
        noWaitingSpaceAvailable  = !added || (waitingCapacity() <= 0);
        if (noWaitingSpaceAvailable) {
            logger.info("Not enough space in memory queue for {}", queuedService);
        }
    }

    public ServerStats getServerStats() {
        ServerStats stats = new ServerStats();
        stats.setWaitingServices(waitingServices.size());
        stats.setAvailableSlots(availableSlots.availablePermits());
        stats.setRunningServicesCount(submittedServicesSet.size());
        stats.setRunningServices(ImmutableList.copyOf(submittedServicesSet));
        return stats;
    }

    void dispatchServices() {
        for (int i = 0; i < MAX_WAITING_SLOTS; i++) {
            if (!availableSlots.tryAcquire()) {
                logger.info("No available processing slots");
                return; // no slot available
            }
            JacsServiceData queuedService = dequeService();
            logger.info("Dequeued service {}", queuedService);
            if (queuedService == null) {
                // nothing to do
                availableSlots.release();
                return;
            }
            logger.info("Dispatch service {}", queuedService);
            ServiceComputation<Object> serviceComputation = (ServiceComputation<Object>) getServiceComputation(queuedService);
            // The service lifecycle is: preprocess -> isReadyToProcess -> processData -> isDone -> postProcess
            CompletableFuture
                    .supplyAsync(() -> queuedService, serviceExecutor)
                    .thenApplyAsync(serviceData -> {
                        logger.debug("Submit {}", serviceData);
                        serviceData.setState(JacsServiceState.SUBMITTED);
                        updateServiceInfo(serviceData);
                        availableSlots.release();
                        return new JacsService<Object>(this, serviceData);
                    }, serviceExecutor)
                    .thenComposeAsync(serviceComputation::preProcessData, serviceExecutor)
                    .thenComposeAsync(serviceComputation::isReadyToProcess, serviceExecutor)
                    .thenComposeAsync(serviceComputation::processData, serviceExecutor)
                    .thenComposeAsync(serviceComputation::isDone, serviceExecutor)
                    .whenCompleteAsync((js, exc) -> {
                        JacsServiceData updatedServiceData;
                        if (js == null) {
                            updatedServiceData = queuedService;
                        } else {
                            updatedServiceData = js.getJacsServiceData();
                        }
                        logger.debug("Complete {}", updatedServiceData);
                        if (exc == null) {
                            logger.info("Successfully completed {}", updatedServiceData);
                            updatedServiceData.setState(JacsServiceState.SUCCESSFUL);
                        } else {
                            logger.error("Error executing {}", updatedServiceData, exc);
                            updatedServiceData.setState(JacsServiceState.ERROR);
                        }
                        updateServiceInfo(updatedServiceData);
                        submittedServicesSet.remove(updatedServiceData.getId());
                    }, serviceExecutor)
                    .whenCompleteAsync((js, exc) -> {
                        JacsService<Object> jacsService;
                        if (js == null) {
                            jacsService = new JacsService<Object>(this, queuedService);
                        } else {
                            jacsService = js;
                        }
                        serviceComputation.postProcessData(jacsService, exc);
                    });
        }
    }

    void syncServiceQueue() {
        logger.info("Sync the waiting queue");
        // check for newly created services and queue them based on their priorities
        enqueueAvailableServices(EnumSet.of(JacsServiceState.CREATED));
    }

    private JacsServiceData dequeService() {
        JacsServiceData queuedService = getWaitingService();
        if (queuedService == null && enqueueAvailableServices(EnumSet.of(JacsServiceState.CREATED, JacsServiceState.QUEUED))) {
            queuedService = getWaitingService();
        }
        return queuedService;
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
                    updateServiceInfo(jacsServiceData);
                }
            }
        } finally {
            queuePermit.release();
        }
        return added;
    }

    private JacsServiceData getWaitingService() {
        try {
            queuePermit.acquireUninterruptibly();
            JacsServiceData jacsServiceData = waitingServices.poll();
            if (jacsServiceData != null) {
                logger.debug("Retrieved waiting service {}", jacsServiceData);
                Long serviceId = jacsServiceData.getId();
                submittedServicesSet.add(serviceId);
                waitingServicesSet.remove(serviceId);
            }
            return jacsServiceData;
        } finally {
            queuePermit.release();
        }
    }

    private void persistServiceInfo(JacsServiceData jacsServiceData) {
        jacsServiceData.setState(JacsServiceState.CREATED);
        jacsServiceDataPersistence.save(jacsServiceData);
    }

    private void updateServiceInfo(JacsServiceData jacsServiceData) {
        jacsServiceDataPersistence.update(jacsServiceData);
    }

    private boolean enqueueAvailableServices(Set<JacsServiceState> jacsServiceStates) {
        int availableSpaces = MAX_WAITING_SLOTS;
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

    private int waitingCapacity() {
        return MAX_WAITING_SLOTS - waitingServices.size();
    }
}
