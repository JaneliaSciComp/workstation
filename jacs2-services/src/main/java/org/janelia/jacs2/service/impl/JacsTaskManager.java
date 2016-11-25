package org.janelia.jacs2.service.impl;


import org.janelia.jacs2.dao.TaskInfoDao;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.service.ServerStats;
import org.janelia.jacs2.service.TaskManager;

import javax.inject.Inject;
import java.util.Optional;

public class JacsTaskManager implements TaskManager {

    private final TaskInfoDao taskInfoDao;
    private final JacsTaskDispatcher taskDispatcher;

    @Inject
    JacsTaskManager(TaskInfoDao taskInfoDao, JacsTaskDispatcher taskDispatcher) {
        this.taskInfoDao = taskInfoDao;
        this.taskDispatcher = taskDispatcher;
    }

    @Override
    public TaskInfo retrieveTaskInfo(Long instanceId) {
        return taskInfoDao.findById(instanceId);
    }

    @Override
    public TaskInfo submitTaskAsync(TaskInfo serviceArgs, Optional<TaskInfo> parentTask) {
        return taskDispatcher.submitTaskAsync(serviceArgs, parentTask);
    }

    @Override
    public ServerStats getServerStats() {
        return taskDispatcher.getServerStats();
    }
}
