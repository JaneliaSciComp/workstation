package org.janelia.jacs2.service.impl;

import com.google.common.base.Preconditions;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.persistence.TaskInfoPersistence;
import org.slf4j.Logger;

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
    private final TaskSupplier<TaskInfo> taskSupplier = new BlockingQueueTaskSupplier<>();
    private TaskInfo taskInfo;

    @Override
    public CompletionStage<TaskInfo> processData() {
        return waitForData()
                .thenApply(this::doWork);
    }

    private CompletionStage<TaskInfo> waitForData() {
        return CompletableFuture.supplyAsync(() -> {
            // bring the service info into the computation by getting it from the supplier channel;
            // the channel will receive the data when the corresponding job is ready to be started
            // from the dispatcher
            if (this.taskInfo == null) {
                this.taskInfo = taskSupplier.take();
            }
            return this.taskInfo;
        });
    }

    protected abstract TaskInfo doWork(TaskInfo ti) throws ComputationException;

    @Override
    public ServiceComputation submitSubTaskAsync(TaskInfo subTaskInfo) {
        logger.debug("Create sub-task {}", subTaskInfo);
        Preconditions.checkState(taskInfo != null, "Children services can only be created once it's running");
        subTaskInfo.setOwner(taskInfo.getOwner());
        return serviceDispatcher.submitService(subTaskInfo, Optional.of(taskInfo));
    }

    @Override
    public TaskSupplier<TaskInfo> getTaskSupplier() {
        return taskSupplier;
    }

}
