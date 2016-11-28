package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.TaskInfo;

import java.util.concurrent.CompletionStage;

/**
 * ServiceComputation represents the actual computation for a specific input.
 */
public interface ServiceComputation {
    /**
     * Pre-processing stage.
     *
     * @return a promise corresponding to the pre-processing stage.
     */
    CompletionStage<TaskInfo> preProcessData(TaskInfo taskInfo);

    /**
     * This is the actual processing method which is performed on the enclosed service data.
     * @return a completion stage that could be chained with other computations
     */
    CompletionStage<TaskInfo> processData(TaskInfo taskInfo);

    /**
     * Check if the task is ready for processing.
     * @param taskInfo task to be checked if it can be processed.
     * @return the corresponding completion stage for this check.
     */
    CompletionStage<TaskInfo> isReady(TaskInfo taskInfo);

    /**
     * Check if the task processing is done.
     * @param taskInfo task to be checked if it is done.
     * @return the corresponding completion stage for this check.
     */
    CompletionStage<TaskInfo> isDone(TaskInfo taskInfo);

    /**
     * Post-processing.
     */
    void postProcessData(TaskInfo taskInfo, Throwable exc);

    /**
     * Submit a sub task.
     * @param subTaskInfo sub task info
     * @param parentTask parent task info
     * @return the computation for the child process.
     */
    ServiceComputation submitSubTaskAsync(TaskInfo subTaskInfo, TaskInfo parentTask);
}
