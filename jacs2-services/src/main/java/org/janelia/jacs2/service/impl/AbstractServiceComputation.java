package org.janelia.jacs2.service.impl;

import com.google.common.base.Preconditions;
import org.janelia.jacs2.model.service.TaskInfo;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public abstract class AbstractServiceComputation implements ServiceComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private JacsTaskDispatcher serviceDispatcher;
    @Inject
    private Executor taskExecutor;
    private final TaskCommChannel<TaskInfo> taskCommChannel = new SingleUsageBlockingQueueTaskCommChannel<>();
    private final TaskCommChannel<TaskInfo> taskResultsChannel = new SingleUsageBlockingQueueTaskCommChannel<>();

    @Override
    public CompletionStage<TaskInfo> processData() {
        return waitForData()
                .thenComposeAsync(this::doWork, taskExecutor)
                .thenApplyAsync(taskInfo -> {
                    taskResultsChannel.put(taskInfo);
                    return taskInfo;
                }, taskExecutor);
    }

    private CompletionStage<TaskInfo> waitForData() {
        return CompletableFuture.supplyAsync(() -> {
            // bring the service info into the computation by getting it from the supplier channel;
            // the channel will receive the data when the corresponding job is ready to be started
            // from the dispatcher
            return taskCommChannel.take();
        });
    }

    protected abstract CompletionStage<TaskInfo> doWork(TaskInfo ti);

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
