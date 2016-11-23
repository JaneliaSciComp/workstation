package org.janelia.jacs2.service.impl;

import com.google.common.base.Preconditions;
import org.janelia.jacs2.model.service.TaskInfo;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public abstract class AbstractServiceComputation implements ServiceComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private JacsTaskDispatcher serviceDispatcher;
    @Inject
    private ExecutorService taskExecutor;
    private TaskCommChannel<TaskInfo> taskBeginChannel;
    private TaskCommChannel<TaskInfo> taskDoneChannel;

    @Override
    public CompletionStage<TaskInfo> processData() {
        return waitForData()
                .thenCompose(this::doWork);
    }

    private CompletionStage<TaskInfo> waitForData() {
        return CompletableFuture.supplyAsync(() -> {
            // bring the service info into the computation by getting it from the supplier channel;
            // the channel will receive the data when the corresponding job is ready to be started
            // from the dispatcher
            TaskInfo ti = getBeginChannel().take();
            logger.info("Task {} is ready", ti);
            return ti;
        }, taskExecutor);
    }

    protected abstract CompletionStage<TaskInfo> doWork(TaskInfo ti);

    @Override
    public ServiceComputation submitSubTaskAsync(TaskInfo subTaskInfo) {
        logger.info("Create sub-task {}", subTaskInfo);
        TaskInfo currentTaskInfo = getBeginChannel().take();
        Preconditions.checkState(currentTaskInfo != null, "Sub tasks can only be created if the parent task is already running");
        subTaskInfo.setOwner(currentTaskInfo.getOwner());
        return serviceDispatcher.submitService(subTaskInfo, Optional.of(currentTaskInfo));
    }

    @Override
    public synchronized TaskCommChannel<TaskInfo> getBeginChannel() {
        if (taskBeginChannel == null) {
            logger.info("Create start task channel");
            taskBeginChannel = new SingleUsageBlockingQueueTaskCommChannel<>();
        }
        return taskBeginChannel;
    }

    @Override
    public synchronized TaskCommChannel<TaskInfo> getDoneChannel() {
        if (taskDoneChannel == null) {
            logger.info("Create end task channel");
            taskDoneChannel = new SingleUsageBlockingQueueTaskCommChannel<>();
        }
        return taskDoneChannel;
    }
}
