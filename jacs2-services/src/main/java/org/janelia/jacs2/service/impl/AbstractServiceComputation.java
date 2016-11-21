package org.janelia.jacs2.service.impl;

import com.google.common.base.Preconditions;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.persistence.TaskInfoPersistence;
import org.slf4j.Logger;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractServiceComputation implements ServiceComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private JacsTaskDispatcher serviceDispatcher;
    @Inject
    private Instance<TaskInfoPersistence> serviceInfoPersistenceSource;
    @Resource
    private ManagedExecutorService managedExecutorService;

    private final TaskCommChannel<TaskInfo> taskCommChannel = new SingleUsageBlockingQueueTaskCommChannel<>();
    private final TaskCommChannel<TaskInfo> taskResultsChannel = new SingleUsageBlockingQueueTaskCommChannel<>();

    @Override
    public CompletionStage<TaskInfo> processData() {
        return waitForData()
                .thenApplyAsync(this::doWork, managedExecutorService)
                .thenApplyAsync(taskInfo -> {
                    taskResultsChannel.put(taskInfo);
                    return taskInfo;
                }, managedExecutorService);
    }

    private CompletionStage<TaskInfo> waitForData() {
        return CompletableFuture.supplyAsync(() -> {
            // bring the service info into the computation by getting it from the supplier channel;
            // the channel will receive the data when the corresponding job is ready to be started
            // from the dispatcher
            return taskCommChannel.take();
        }, managedExecutorService);
    }

    protected abstract TaskInfo doWork(TaskInfo ti) throws ComputationException;

    @Override
    public ServiceComputation submitSubTaskAsync(TaskInfo subTaskInfo) {
        logger.debug("Create sub-task {}", subTaskInfo);
        TaskInfo currentTaskInfo = taskCommChannel.take();
        Preconditions.checkState(currentTaskInfo != null, "Sub tasks can only be created if the parent task is already running");
        subTaskInfo.setOwner(currentTaskInfo.getOwner());
        return serviceDispatcher.submitService(subTaskInfo, Optional.of(currentTaskInfo));
    }

    @Override
    public TaskCommChannel<TaskInfo> getReadyChannel() {
        return taskCommChannel;
    }

    @Override
    public TaskCommChannel<TaskInfo> getResultsChannel() {
        return taskResultsChannel;
    }
}
