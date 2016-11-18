package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.TaskInfo;

/**
 * This contains the task with the corresponding computation.
 */
class QueuedTask {
    private final TaskInfo taskInfo;
    private final ServiceComputation serviceComputation;

    QueuedTask(TaskInfo taskInfo, ServiceComputation serviceComputation) {
        this.taskInfo = taskInfo;
        this.serviceComputation = serviceComputation;
    }

    TaskInfo getTaskInfo() {
        return taskInfo;
    }

    ServiceComputation getServiceComputation() {
        return serviceComputation;
    }
}
