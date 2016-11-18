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
     * @return this is the communication component of the current service through which data is entered in the computation.
     */
    TaskCommChannel<TaskInfo> getReadyChannel();

    /**
     * @return the channel used for communicating that results are available.
     */
    TaskCommChannel<TaskInfo> getResultsChannel();
}
