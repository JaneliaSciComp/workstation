package org.janelia.jacs2.service.impl;

import com.google.common.collect.ImmutableList;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.model.service.TaskState;
import org.janelia.jacs2.persistence.TaskInfoPersistence;
import org.janelia.jacs2.service.ServerStats;
import org.janelia.jacs2.service.ServiceRegistry;
import org.slf4j.Logger;

import javax.annotation.Resource;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
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
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

@Startup
@Singleton
public class JacsTaskDispatcher {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_WAITING_SLOTS = 100;
    private static final int MAX_RUNNING_SLOTS = 200;

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Resource
    private ManagedExecutorService managedExecutorService;
    @Inject
    private Instance<TaskInfoPersistence> taskInfoPersistenceSource;
    @Inject
    private Instance<ServiceRegistry> serviceRegistrarSource;
    private final Queue<QueuedTask> waitingTasks;
    private final Semaphore availableSlots;
    private boolean noWaitingSpaceAvailable;
    private int runningServices;

    public JacsTaskDispatcher() {
        availableSlots = new Semaphore(MAX_RUNNING_SLOTS, true);
        waitingTasks = new PriorityBlockingQueue<>(MAX_WAITING_SLOTS, new DefaultServiceInfoComparator());
        noWaitingSpaceAvailable = false;
    }

    public ServiceComputation submitService(TaskInfo serviceArgs, Optional<TaskInfo> currentService) {
        ServiceDescriptor serviceDescriptor = getServiceDescriptor(serviceArgs.getName());
        ServiceComputation serviceComputation = serviceDescriptor.createComputationInstance();
        serviceArgs.setServiceType(ServiceComputation.class.getName());
        if (currentService.isPresent()) {
            serviceArgs.updateParentTask(currentService.get());
        }
        TaskInfoPersistence taskInfoPersistence = taskInfoPersistenceSource.get();
        persistServiceInfo(taskInfoPersistence, serviceArgs);
        enqueueService(taskInfoPersistence, new QueuedTask(serviceArgs, serviceComputation));
        return serviceComputation;
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

    private void enqueueService(TaskInfoPersistence taskInfoPersistence, QueuedTask service) {
        if (noWaitingSpaceAvailable) {
            // don't even check if anything has become available since last time
            // just drop it for now - the queue will be refilled after it drains.
            logger.info("In memory queue reached the capacity so service {} will not be put in memory", service.getTaskInfo());
            return;
        }
        boolean added = addWaitingService(taskInfoPersistence, service);
        noWaitingSpaceAvailable  = !added || (waitingCapacity() <= 0);
        if (noWaitingSpaceAvailable) {
            logger.info("Not enough space in memory queue for {}", service.getTaskInfo());
        }
    }

    private boolean addWaitingService(TaskInfoPersistence taskInfoPersistence, QueuedTask service) {
        boolean added = waitingTasks.offer(service);
        if (added) {
            TaskInfo si = service.getTaskInfo();
            if (si.getState() == TaskState.CREATED) {
                si.setState(TaskState.QUEUED);
                updateServiceInfo(taskInfoPersistence, si);
            }
        }
        return added;
    }

    public ServerStats getServerStats() {
        ServerStats stats = new ServerStats();
        stats.setWaitingTasks(waitingTasks.size());
        stats.setAvailableSlots(availableSlots.availablePermits());
        stats.setRunningTasks(runningServices);
        return stats;
    }

    void dispatchServices() {
        for (int i = 0; i < BATCH_SIZE; i++) {
            if (!availableSlots.tryAcquire()) {
                logger.debug("No available processing slots");
                return; // no slot available
            }
            TaskInfoPersistence taskInfoPersistence = taskInfoPersistenceSource.get();
            QueuedTask service = dequeService(taskInfoPersistence);
            if (service == null) {
                // nothing to do
                availableSlots.release();
                return;
            }
            CompletableFuture
                    .supplyAsync(() -> {
                        runningServices++;
                        TaskInfo si = service.getTaskInfo();
                        service.getServiceComputation().getTaskSupplier().put(si);
                        return service;
                    }, managedExecutorService)
                    .thenApply(sc -> {
                        TaskInfo si = sc.getTaskInfo();
                        logger.debug("Submit {}" + si);
                        si.setState(TaskState.SUBMITTED);
                        updateServiceInfo(taskInfoPersistence, si);
                        return sc;
                    })
                    .thenComposeAsync(sc -> sc.getServiceComputation().processData(), managedExecutorService)
                    .whenComplete((si, exc) -> {
                        availableSlots.release();
                        runningServices--;
                        if (exc == null) {
                            logger.info("Successfully completed {}", si);
                            si.setState(TaskState.SUCCESSFUL);
                        } else {
                            logger.error("Error executing {}", si, exc);
                            si.setState(TaskState.ERROR);
                        }
                        updateServiceInfo(taskInfoPersistence, si);
                    });
        }
    }

    void syncServiceQueue() {
        if (noWaitingSpaceAvailable) {
            logger.info("Sync the waiting queue");
            // if at any point we reached the capacity of the in memory waiting queue
            // synchronize the in memory queue with the database and fill the queue with services that are still in CREATED state
            enqueueAvailableServices(taskInfoPersistenceSource.get(), EnumSet.of(TaskState.CREATED));
        }
    }

    private QueuedTask dequeService(TaskInfoPersistence taskInfoPersistence) {
        QueuedTask service = waitingTasks.poll();
        if (service == null && enqueueAvailableServices(taskInfoPersistence, EnumSet.of(TaskState.CREATED, TaskState.QUEUED))) {
            service = waitingTasks.poll();
        }
        return service;
    }

    private void persistServiceInfo(TaskInfoPersistence taskInfoPersistence, TaskInfo si) {
        si.setState(TaskState.CREATED);
        taskInfoPersistence.save(si);
        logger.info("Created service {}", si);
    }

    private void updateServiceInfo(TaskInfoPersistence taskInfoPersistence, TaskInfo si) {
        taskInfoPersistence.update(si);
        logger.info("Updated service {}", si);
    }

    private boolean enqueueAvailableServices(TaskInfoPersistence taskInfoPersistence, Set<TaskState> taskStates) {
        int availableSpaces = waitingCapacity();
        if (availableSpaces <= 0) {
            return false;
        }
        PageRequest servicePageRequest = new PageRequest();
        servicePageRequest.setPageSize(availableSpaces);
        servicePageRequest.setSortCriteria(new ArrayList<>(ImmutableList.of(
                new SortCriteria("priority", SortDirection.DESC),
                new SortCriteria("creationDate"))));
        PageResult<TaskInfo> services = taskInfoPersistence.findServicesByState(taskStates, servicePageRequest);
        if (services.getResultList().size() > 0) {
            services.getResultList().stream().forEach(si -> {
                try {
                    ServiceDescriptor serviceDescriptor = getServiceDescriptor(si.getName());
                    ServiceComputation serviceComputation = serviceDescriptor.createComputationInstance();
                    addWaitingService(taskInfoPersistence, new QueuedTask(si, serviceComputation));
                } catch (Exception e) {
                    logger.error("Internal error - no computation can be created for {}", si);
                }
            });
            noWaitingSpaceAvailable = waitingCapacity() <= 0;
            return true;
        }
        return false;
    }

    private int waitingCapacity() {
        return MAX_WAITING_SLOTS - waitingTasks.size();
    }
}
