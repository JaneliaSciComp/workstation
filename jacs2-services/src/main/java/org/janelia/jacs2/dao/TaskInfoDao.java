package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.model.service.TaskState;

import java.util.List;
import java.util.Set;

public interface TaskInfoDao extends Dao<TaskInfo, Long> {
    PageResult<TaskInfo> findTasksByState(Set<TaskState> requestStates, PageRequest pageRequest);
    List<TaskInfo> findSubTasks(Long taskId);
    List<TaskInfo> findTaskHierarchy(Long taskId);
}
