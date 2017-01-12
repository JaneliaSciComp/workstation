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
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@Singleton
public class JacsServiceDispatcher {

    private static final int MAX_WAITING_SLOTS = 20;
    private static final int MAX_RUNNING_SLOTS = 1000;

    private final ExecutorService serviceExecutor;
    private final JacsServiceDataPersistence jacsServiceDataPersistence;
    private final Instance<ServiceRegistry> serviceRegistrarSource;
    private final Logger logger;
    private final ServiceComputationFactory serviceComputationFactory;
    private final Queue<JacsServiceData> waitingServices;
    private final Set<Number> waitingServicesSet = new ConcurrentSkipListSet<>();
    private final Set<Number> submittedServicesSet = new ConcurrentSkipListSet<>();
    private final Semaphore queuePermit;
    private int nAvailableSlots;
    private final Semaphore availableSlots;
    private boolean noWaitingSpaceAvailable;

    @Inject
    public JacsServiceDispatcher(ExecutorService serviceExecutor, JacsServiceDataPersistence jacsServiceDataPersistence, Instance<ServiceRegistry> serviceRegistrarSource, Logger logger) {
        this.serviceExecutor = serviceExecutor;
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
        this.serviceRegistrarSource = serviceRegistrarSource;
        this.logger = logger;
        serviceComputationFactory = new ServiceComputationFactory(this.serviceExecutor);
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

    public JacsServiceData submitServiceAsync(JacsServiceData serviceData) {
        logger.info("Submitted {}", serviceData);
        persistServiceInfo(serviceData);
        enqueueService(serviceData);
        return serviceData;
    }

    ServiceProcessor<?> getServiceProcessor(JacsServiceData jacsServiceData) {
        ServiceDescriptor serviceDescriptor = getServiceDescriptor(jacsServiceData.getName());
        return serviceDescriptor.createServiceProcessor();
    }

//    <S, R> ServiceComputation<S, R> getServiceComputation(JacsServiceData jacsServiceData) {
//        ServiceDescriptor serviceDescriptor = getServiceDescriptor(jacsServiceData.getName());
//        return serviceComputationFactory.createServiceComputation(serviceDescriptor.createServiceProcessor(), jacsServiceData);
//    }

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
            ServiceProcessor<Object> serviceProcessor = (ServiceProcessor<Object>) getServiceProcessor(queuedService);
            serviceComputationFactory.newComputation(null)
                    .applyFunction(o -> (Function<Void, JacsService>) nothing -> {
                        JacsServiceData updatedService = queuedService;
                        logger.debug("Submit {}", updatedService);
                        updatedService.setState(JacsServiceState.SUBMITTED);
                        updateServiceInfo(updatedService);
                        availableSlots.release();
                        return new JacsService(this, updatedService);
                    })
                    .thenCompose(serviceProcessor, (sp, sd) -> sp.process((JacsService) sd))
                    .whenComplete((r, exc) -> {
                        JacsServiceData updatedServiceData = queuedService;
                        if (exc == null) {
                            logger.info("Successfully completed {}", updatedServiceData);
                            updatedServiceData.setState(JacsServiceState.SUCCESSFUL);
                        } else {
                            // if the service data state has already been marked as cancelled or error leave it as is
                            if (!updatedServiceData.hasCompletedUnsuccessfully()) {
                                logger.error("Error executing {}", updatedServiceData, exc);
                                updatedServiceData.setState(JacsServiceState.ERROR);
                            }
                        }
                        updateServiceInfo(updatedServiceData);
                        submittedServicesSet.remove(updatedServiceData.getId());
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
                Number serviceId = jacsServiceData.getId();
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
        jacsServiceDataPersistence.saveHierarchy(jacsServiceData);
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
