package org.janelia.jacs2.service.impl;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.janelia.jacs2.model.service.TaskInfo;

import java.util.Arrays;

/**
 * This contains the queued task with the corresponding computation.
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

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
