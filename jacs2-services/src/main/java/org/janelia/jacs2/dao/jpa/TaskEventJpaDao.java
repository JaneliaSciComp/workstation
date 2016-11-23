package org.janelia.jacs2.dao.jpa;

import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.cdi.qualifier.ComputePersistence;
import org.janelia.jacs2.dao.TaskEventDao;
import org.janelia.jacs2.model.service.TaskEvent;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * JPA implementation of TaskEventDao
 */
public class TaskEventJpaDao extends AbstractJpaDao<TaskEvent, Long> implements TaskEventDao {

    @Inject
    TaskEventJpaDao(@ComputePersistence EntityManager em) {
        super(em);
    }

    @Override
    public List<TaskEvent> findAllEventsByTaskId(Long taskId) {
        String query = "select e from TaskEvent e where e.taskInfo.id = :taskId order by e.eventTime";
        return findByQueryParams(query, ImmutableMap.<String, Object>of("taskId", taskId), TaskEvent.class);
    }
}
