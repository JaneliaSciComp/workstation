package org.janelia.jacs2.dao.jpa;

import org.janelia.jacs2.dao.TaskEventDao;
import org.janelia.jacs2.model.service.TaskEvent;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TaskEventJpaDaoTest {

    @Test
    public void getAllEventsByServiceId() {
        EntityManager mockEm = mock(EntityManager.class);
        TaskEventDao testDao = new TaskEventJpaDao(mockEm);
        TypedQuery<TaskEvent> mockQuery = (TypedQuery<TaskEvent>) mock(TypedQuery.class);
        when(mockEm.createQuery(anyString(), same(TaskEvent.class))).thenReturn(mockQuery);
        Long testTaskId = 1L;
        testDao.findAllEventsByTaskId(testTaskId);
        verify(mockEm).createQuery("select e from TaskEvent e where e.taskInfo.id = :taskId order by e.eventTime", TaskEvent.class);
        verify(mockQuery).setParameter("taskId", testTaskId);
        verify(mockQuery).getResultList();
        verifyNoMoreInteractions(mockQuery);
    }

}
