package org.janelia.jacs2.persistence;

import org.janelia.jacs2.dao.TaskInfoDao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.model.service.TaskState;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Set;

public class TaskInfoPersistence extends AbstractDataPersistence<TaskInfoDao, TaskInfo, Long> {

    @Inject
    public TaskInfoPersistence(Instance<TaskInfoDao> daoSource) {
        super(daoSource);
    }

    public PageResult<TaskInfo> findTasksByState(Set<TaskState> requestStates, PageRequest pageRequest) {
        return daoSource.get().findTasksByState(requestStates, pageRequest);
    }

}
