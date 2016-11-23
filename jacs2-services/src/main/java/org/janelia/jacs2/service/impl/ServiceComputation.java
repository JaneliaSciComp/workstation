package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.TaskInfo;

import java.util.concurrent.CompletionStage;

/**
 * ServiceComputation represents the actual computation for a specific input.
 */
public interface ServiceComputation {
    /**
     * This is the actual processing method which is performed on the enclosed service data.
     * @return a completion stage that could be chained with other computations
     */
    CompletionStage<TaskInfo> processData();

    /**
     *
     * @param subTaskInfo child service info
     * @return the computation for the child process.
     */
    ServiceComputation submitSubTaskAsync(TaskInfo subTaskInfo);

    /**
     * @return the channel used for communicating that the task can begin processing.
     */
    TaskCommChannel<TaskInfo> getBeginChannel();

    /**
     * @return the channel used for communicating that the task completed.
     */
    TaskCommChannel<TaskInfo> getDoneChannel();
}
