package org.janelia.jacs2.persistence;

import org.janelia.jacs2.dao.TaskInfoDao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.model.service.TaskState;

import javax.inject.Inject;
import java.util.Set;

public class TaskInfoPersistence extends AbstractDataPersistence<TaskInfoDao, TaskInfo, Long> {

    @Inject
    public TaskInfoPersistence(TaskInfoDao dao) {
        super(dao);
    }

    public PageResult<TaskInfo> findServicesByState(Set<TaskState> requestStates, PageRequest pageRequest) {
        return dao.findTasksByState(requestStates, pageRequest);
    }

}
