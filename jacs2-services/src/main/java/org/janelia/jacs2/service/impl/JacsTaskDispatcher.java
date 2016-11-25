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
public class JacsTaskDispatcher {

    private static final int MAX_WAITING_SLOTS = 20;
    private static final int MAX_RUNNING_SLOTS = 1000;

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private ExecutorService taskExecutor;
    @Inject
    private TaskInfoPersistence taskInfoPersistence;
    @Inject
    private Instance<ServiceRegistry> serviceRegistrarSource;
    private final Queue<TaskInfo> waitingTasks;
    private final Set<Long> waitingTaskSet = new ConcurrentSkipListSet<>();
    private final Set<Long> submittedTaskSet = new ConcurrentSkipListSet<>();
    private final Semaphore queuePermit;
    private int nAvailableSlots;
    private final Semaphore availableSlots;
    private boolean noWaitingSpaceAvailable;

    public JacsTaskDispatcher() {
        queuePermit = new Semaphore(1, true);
        nAvailableSlots = MAX_RUNNING_SLOTS;
        availableSlots = new Semaphore(nAvailableSlots, true);
        waitingTasks = new PriorityBlockingQueue<>(MAX_WAITING_SLOTS, new DefaultServiceInfoComparator());
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

    public TaskInfo submitTaskAsync(TaskInfo serviceTask, Optional<TaskInfo> optionalParentTask) {
        logger.info("Submitted {} as a {}", serviceTask, optionalParentTask.isPresent() ? "sub-task of " + optionalParentTask.get().getId() : "root task");
        if (optionalParentTask.isPresent()) {
            serviceTask.updateParentTask(optionalParentTask.get());
        }
        persistServiceInfo(serviceTask);
        enqueueService(serviceTask);
        return serviceTask;
    }

    ServiceComputation getServiceComputation(TaskInfo taskInfo) {
        ServiceDescriptor serviceDescriptor = getServiceDescriptor(taskInfo.getName());
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

    private void enqueueService(TaskInfo queuedTask) {
        if (noWaitingSpaceAvailable) {
            // don't even check if anything has become available since last time
            // just drop it for now - the queue will be refilled after it drains.
            logger.info("In memory queue reached the capacity so service {} will not be put in memory", queuedTask);
            return;
        }
        boolean added = addWaitingTask(queuedTask);
        noWaitingSpaceAvailable  = !added || (waitingCapacity() <= 0);
        if (noWaitingSpaceAvailable) {
            logger.info("Not enough space in memory queue for {}", queuedTask);
        }
    }

    public ServerStats getServerStats() {
        ServerStats stats = new ServerStats();
        stats.setWaitingTasks(waitingTasks.size());
        stats.setAvailableSlots(availableSlots.availablePermits());
        stats.setRunningTasksCount(submittedTaskSet.size());
        stats.setRunningTasks(ImmutableList.copyOf(submittedTaskSet));
        return stats;
    }

    void dispatchServices() {
        for (int i = 0; i < MAX_WAITING_SLOTS; i++) {
            if (!availableSlots.tryAcquire()) {
                logger.debug("No available processing slots");
                return; // no slot available
            }
            TaskInfo queuedTask = dequeTask();
            logger.info("Dequeued task {}", queuedTask);
            if (queuedTask == null) {
                // nothing to do
                availableSlots.release();
                return;
            }
            logger.info("Dispatch task {}", queuedTask);
            ServiceComputation serviceComputation = getServiceComputation(queuedTask);
            CompletableFuture
                    .supplyAsync(() -> queuedTask, taskExecutor)
                    .thenApplyAsync(ti -> {
                        logger.debug("Submit {}", ti);
                        ti.setState(TaskState.SUBMITTED);
                        updateServiceInfo(ti);
                        availableSlots.release();
                        return ti;
                    }, taskExecutor)
                    .thenComposeAsync(serviceComputation::isReady, taskExecutor)
                    .thenComposeAsync(serviceComputation::processData, taskExecutor)
                    .thenComposeAsync(serviceComputation::isDone, taskExecutor)
                    .whenCompleteAsync((taskInfo, exc) -> {
                        logger.debug("Complete {}", taskInfo);
                        if (exc == null) {
                            logger.info("Successfully completed {}", taskInfo);
                            taskInfo.setState(TaskState.SUCCESSFUL);
                        } else {
                            logger.error("Error executing {}", taskInfo, exc);
                            taskInfo.setState(TaskState.ERROR);
                        }
                        updateServiceInfo(taskInfo);
                        submittedTaskSet.remove(taskInfo.getId());
                    }, taskExecutor);
        }
    }

    void syncServiceQueue() {
        if (noWaitingSpaceAvailable) {
            logger.info("Sync the waiting queue");
            // if at any point we reached the capacity of the in memory waiting queue
            // synchronize the in memory queue with the database and fill the queue with services that are still in CREATED state
            enqueueAvailableServices(EnumSet.of(TaskState.CREATED));
        }
    }

    private TaskInfo dequeTask() {
        TaskInfo queuedTask = getWaitingTask();
        if (queuedTask == null && enqueueAvailableServices(EnumSet.of(TaskState.CREATED, TaskState.QUEUED))) {
            queuedTask = getWaitingTask();
        }
        return queuedTask;
    }

    private boolean addWaitingTask(TaskInfo taskInfo) {
        boolean added = false;
        try {
            queuePermit.acquireUninterruptibly();
            if (submittedTaskSet.contains(taskInfo.getId()) || waitingTaskSet.contains(taskInfo.getId())) {
                // task is already waiting or running
                return true;
            }
            added = waitingTasks.offer(taskInfo);
            if (added) {
                logger.debug("Enqueued task {}", taskInfo);
                waitingTaskSet.add(taskInfo.getId());
                if (taskInfo.getState() == TaskState.CREATED) {
                    taskInfo.setState(TaskState.QUEUED);
                    updateServiceInfo(taskInfo);
                }
            }
        } finally {
            queuePermit.release();
        }
        return added;
    }

    private TaskInfo getWaitingTask() {
        try {
            queuePermit.acquireUninterruptibly();
            TaskInfo taskInfo = waitingTasks.poll();
            if (taskInfo != null) {
                logger.debug("Retrieved waiting task {}", taskInfo);
                Long taskId = taskInfo.getId();
                submittedTaskSet.add(taskId);
                waitingTaskSet.remove(taskId);
            }
            return taskInfo;
        } finally {
            queuePermit.release();
        }
    }

    private void persistServiceInfo(TaskInfo taskInfo) {
        taskInfo.setState(TaskState.CREATED);
        taskInfoPersistence.save(taskInfo);
    }

    private void updateServiceInfo(TaskInfo taskInfo) {
        taskInfoPersistence.update(taskInfo);
    }

    private boolean enqueueAvailableServices(Set<TaskState> taskStates) {
        int availableSpaces = MAX_WAITING_SLOTS;
        PageRequest servicePageRequest = new PageRequest();
        servicePageRequest.setPageSize(availableSpaces);
        servicePageRequest.setSortCriteria(new ArrayList<>(ImmutableList.of(
                new SortCriteria("priority", SortDirection.DESC),
                new SortCriteria("creationDate"))));
        PageResult<TaskInfo> services = taskInfoPersistence.findTasksByState(taskStates, servicePageRequest);
        if (services.getResultList().size() > 0) {
            services.getResultList().stream().forEach(taskInfo -> {
                try {
                    if (!submittedTaskSet.contains(taskInfo.getId()) && !waitingTaskSet.contains(taskInfo.getId())) {
                        addWaitingTask(taskInfo);
                    }
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
