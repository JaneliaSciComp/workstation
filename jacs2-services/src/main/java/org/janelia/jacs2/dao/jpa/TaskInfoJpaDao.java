package org.janelia.jacs2.dao.jpa;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.cdi.qualifier.ComputePersistence;
import org.janelia.jacs2.dao.TaskInfoDao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.model.service.TaskState;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JPA implementation of TaskInfoDao
 */
public class TaskInfoJpaDao extends AbstractJpaDao<TaskInfo, Long> implements TaskInfoDao {

    @Inject
    TaskInfoJpaDao(@ComputePersistence EntityManager em) {
        super(em);
    }

    @Override
    public List<TaskInfo> findSubTasks(Long taskId) {
        String query = "select ti from TaskInfo ti where ti.parentTaskId = :parentTaskId";
        return findByQueryParams(query, ImmutableMap.<String, Object>of("parentTaskId", taskId), TaskInfo.class);
    }

    @Override
    public List<TaskInfo> findTaskHierarchy(Long taskId) {
        TaskInfo taskInfo = findById(taskId);
        Preconditions.checkArgument(taskInfo != null, "Invalid task ID - no task found for " + taskId);
        Long rootTaskId = taskInfo.getRootTaskId();
        if (rootTaskId == null) {
            rootTaskId = taskId;
        }
        String query = "select ti from TaskInfo ti where ti.rootTaskId = :rootTaskId order by id ";
        List<TaskInfo> fullTaskHierachy = findByQueryParams(query, ImmutableMap.<String, Object>of("rootTaskId", rootTaskId), TaskInfo.class);
        List<TaskInfo> taskHierarchy = new ArrayList<>();
        Set<Long> taskHierarchySet = new HashSet<>();
        taskHierarchySet.add(taskId);
        fullTaskHierachy.stream().forEach(ti -> {
            if (taskHierarchySet.contains(ti.getParentTaskId())) {
                taskHierarchy.add(ti);
                taskHierarchySet.add(ti.getId());
            }
        });
        return taskHierarchy;
    }

    @Override
    public PageResult<TaskInfo> findTasksByState(Set<TaskState> requestStates, PageRequest pageRequest) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(requestStates));
        String query = "select ti from TaskInfo ti where ti.state in :taskStateValues " + getOrderByStatement(pageRequest.getSortCriteria());
        List<TaskInfo> results = findByQueryParamsWithPaging(query,
                ImmutableMap.<String, Object>of("taskStateValues", requestStates),
                pageRequest.getOffset(),
                pageRequest.getPageSize(),
                TaskInfo.class);
        return new PageResult<>(pageRequest, results);
    }
}
