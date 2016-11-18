package org.janelia.jacs2.service;

import org.janelia.jacs2.model.service.TaskInfo;

import java.util.Optional;

public interface TaskManager {
    TaskInfo retrieveTaskInfo(Long instanceId);
    TaskInfo submitTaskAsync(TaskInfo serviceArgs, Optional<TaskInfo> parentTask);
    ServerStats getServerStats();
}
