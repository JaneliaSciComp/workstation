package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.persistence.TaskInfoPersistence;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public abstract class AbstractServiceComputation implements ServiceComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private JacsTaskDispatcher serviceDispatcher;
    @Inject
    private TaskInfoPersistence taskInfoPersistence;

    @Override
    public CompletionStage<TaskInfo> isReady(TaskInfo taskInfo) {
        CompletableFuture<TaskInfo> checkIfReadyFuture = new CompletableFuture<>();
        List<TaskInfo> uncompletedSubTasks = taskInfoPersistence.findTaskHierarchy(taskInfo.getId());
        for (;;) {
            if (uncompletedSubTasks.isEmpty()) {
                checkIfReadyFuture.complete(taskInfo);
                break;
            }
            List<TaskInfo> firstPass =  uncompletedSubTasks.stream()
                .map(ti -> taskInfoPersistence.findById(ti.getId()))
                .filter(ti -> !ti.hasCompleted())
                .collect(Collectors.toList());
            uncompletedSubTasks = firstPass;
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                logger.warn("Interrup {}", taskInfo, e);
                checkIfReadyFuture.completeExceptionally(e);
                break;
            }
        }
        return checkIfReadyFuture;
    }

    @Override
    public CompletionStage<TaskInfo> isDone(TaskInfo taskInfo) {
        CompletableFuture<TaskInfo> checkIfDoneFuture = new CompletableFuture<>();
        checkIfDoneFuture.complete(taskInfo);
        return checkIfDoneFuture;
    }

    @Override
    public ServiceComputation submitSubTaskAsync(TaskInfo subTaskInfo, TaskInfo parentTask) {
        logger.info("Create sub-task {}", subTaskInfo);
        subTaskInfo.setOwner(parentTask.getOwner());
        TaskInfo task = serviceDispatcher.submitTaskAsync(subTaskInfo, Optional.of(parentTask));
        return serviceDispatcher.getServiceComputation(task);
    }

}
