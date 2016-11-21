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
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

@Startup
@Singleton
public class JacsTaskDispatcher {

    private static final int MAX_WAITING_SLOTS = 20;
    private static final int MAX_RUNNING_SLOTS = 100;

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
    private final Set<Long> waitingTaskSet = new ConcurrentSkipListSet<>();
    private final Set<Long> submittedTaskSet = new ConcurrentSkipListSet<>();
    private final Semaphore queuePermit;
    private final Semaphore availableSlots;
    private boolean noWaitingSpaceAvailable;

    public JacsTaskDispatcher() {
        queuePermit = new Semaphore(1, true);
        availableSlots = new Semaphore(MAX_RUNNING_SLOTS, true);
        waitingTasks = new PriorityBlockingQueue<>(MAX_WAITING_SLOTS, new DefaultServiceInfoComparator());
        noWaitingSpaceAvailable = false;
    }

    public ServiceComputation submitService(TaskInfo serviceArgs, Optional<TaskInfo> currentService) {
        logger.info("Submitted {} as a {}", serviceArgs, currentService.isPresent() ? "sub-task of " + currentService.get().getId() : "root task");
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
        boolean added = addWaitingTask(taskInfoPersistence, service);
        noWaitingSpaceAvailable  = !added || (waitingCapacity() <= 0);
        if (noWaitingSpaceAvailable) {
            logger.info("Not enough space in memory queue for {}", service.getTaskInfo());
        }
    }

    public ServerStats getServerStats() {
        ServerStats stats = new ServerStats();
        stats.setWaitingTasks(waitingTasks.size());
        stats.setAvailableSlots(availableSlots.availablePermits());
        stats.setRunningTasks(submittedTaskSet.size());
        return stats;
    }

    void dispatchServices() {
        for (int i = 0; i < MAX_WAITING_SLOTS; i++) {
            if (!availableSlots.tryAcquire()) {
                logger.debug("No available processing slots");
                return; // no slot available
            }
            TaskInfoPersistence taskInfoPersistence = taskInfoPersistenceSource.get();
            QueuedTask queuedTask = dequeTask(taskInfoPersistence);
            if (queuedTask == null) {
                // nothing to do
                availableSlots.release();
                return;
            }
            CompletableFuture
                    .supplyAsync(() -> {
                        TaskInfo taskInfo = queuedTask.getTaskInfo();
                        queuedTask.getServiceComputation().getReadyChannel().put(taskInfo);
                        return queuedTask;
                    }, managedExecutorService)
                    .thenApplyAsync(sc -> {
                        TaskInfo taskInfo = sc.getTaskInfo();
                        logger.debug("Submit {}", taskInfo);
                        taskInfo.setState(TaskState.SUBMITTED);
                        updateServiceInfo(taskInfoPersistence, taskInfo);
                        return sc;
                    }, managedExecutorService)
                    .thenComposeAsync(sc -> sc.getServiceComputation().processData(), managedExecutorService)
                    .whenCompleteAsync((taskInfo, exc) -> {
                        availableSlots.release();
                        if (exc == null) {
                            logger.info("Successfully completed {}", taskInfo);
                            taskInfo.setState(TaskState.SUCCESSFUL);
                        } else {
                            logger.error("Error executing {}", taskInfo, exc);
                            taskInfo.setState(TaskState.ERROR);
                        }
                        updateServiceInfo(taskInfoPersistence, taskInfo);
                        submittedTaskSet.remove(taskInfo.getId());
                    }, managedExecutorService);
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

    private QueuedTask dequeTask(TaskInfoPersistence taskInfoPersistence) {
        QueuedTask queuedTask = getWaitingTask();
        if (queuedTask == null && enqueueAvailableServices(taskInfoPersistence, EnumSet.of(TaskState.CREATED, TaskState.QUEUED))) {
            queuedTask = getWaitingTask();
        }
        return queuedTask;
    }

    private boolean addWaitingTask(TaskInfoPersistence taskInfoPersistence, QueuedTask queuedTask) {
        boolean added = false;
        try {
            queuePermit.acquireUninterruptibly();
            TaskInfo taskInfo = queuedTask.getTaskInfo();
            if (submittedTaskSet.contains(taskInfo.getId()) || waitingTaskSet.contains(taskInfo.getId())) {
                // task is already waiting or running
                return true;
            }
            added = waitingTasks.offer(queuedTask);
            if (added) {
                logger.debug("Enqueued task {}", taskInfo);
                waitingTaskSet.add(taskInfo.getId());
                if (taskInfo.getState() == TaskState.CREATED) {
                    taskInfo.setState(TaskState.QUEUED);
                    updateServiceInfo(taskInfoPersistence, taskInfo);
                }
            }
        } finally {
            queuePermit.release();
        }
        return added;
    }

    private QueuedTask getWaitingTask() {
        try {
            queuePermit.acquireUninterruptibly();
            QueuedTask queuedTask = waitingTasks.poll();
            if (queuedTask != null) {
                Long taskId = queuedTask.getTaskInfo().getId();
                submittedTaskSet.add(taskId);
                waitingTaskSet.remove(taskId);
            }
            return queuedTask;
        } finally {
            queuePermit.release();
        }
    }

    private void clearWaitingQueue() {
        try {
            queuePermit.acquireUninterruptibly();
            waitingTasks.clear();
        } finally {
            queuePermit.release();
        }
    }

    private void persistServiceInfo(TaskInfoPersistence taskInfoPersistence, TaskInfo taskInfo) {
        taskInfo.setState(TaskState.CREATED);
        taskInfoPersistence.save(taskInfo);
        logger.info("Created task {}", taskInfo);
    }

    private void updateServiceInfo(TaskInfoPersistence taskInfoPersistence, TaskInfo taskInfo) {
        taskInfoPersistence.update(taskInfo);
        logger.info("Updated task {}", taskInfo);
    }

    private boolean enqueueAvailableServices(TaskInfoPersistence taskInfoPersistence, Set<TaskState> taskStates) {
        clearWaitingQueue();
        int availableSpaces = waitingCapacity();
        if (availableSpaces <= 0) {
            return false;
        }
        PageRequest servicePageRequest = new PageRequest();
        servicePageRequest.setPageSize(availableSpaces);
        servicePageRequest.setSortCriteria(new ArrayList<>(ImmutableList.of(
                new SortCriteria("priority", SortDirection.DESC),
                new SortCriteria("creationDate"))));
        PageResult<TaskInfo> services = taskInfoPersistence.findTasksByState(taskStates, servicePageRequest);
        if (services.getResultList().size() > 0) {
            services.getResultList().stream().forEach(taskInfo -> {
                try {
                    ServiceDescriptor serviceDescriptor = getServiceDescriptor(taskInfo.getName());
                    ServiceComputation serviceComputation = serviceDescriptor.createComputationInstance();
                    addWaitingTask(taskInfoPersistence, new QueuedTask(taskInfo, serviceComputation));
                } catch (Exception e) {
                    logger.error("Internal error - no computation can be created for {}", taskInfo);
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
