package org.janelia.jacs2.persistence;

import org.janelia.jacs2.dao.TaskInfoDao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.model.service.TaskState;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;

public class TaskInfoPersistence extends AbstractDataPersistence<TaskInfoDao, TaskInfo, Long> {

    @Inject
    public TaskInfoPersistence(Instance<TaskInfoDao> taskDaoSource) {
        super(taskDaoSource);
    }

    public PageResult<TaskInfo> findTasksByState(Set<TaskState> requestStates, PageRequest pageRequest) {
        TaskInfoDao  taskDao = daoSource.get();
        try {
            return taskDao.findTasksByState(requestStates, pageRequest);
        } finally {
            daoSource.destroy(taskDao);
        }
    }

    public List<TaskInfo> findTaskHierarchy(Long taskId) {
        TaskInfoDao  taskDao = daoSource.get();
        try {
            return taskDao.findTaskHierarchy(taskId);
        } finally {
            daoSource.destroy(taskDao);
        }
    }
}
