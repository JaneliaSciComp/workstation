package org.janelia.jacs2.dao.jpa;

import org.janelia.jacs2.dao.JacsServiceEventDao;
import org.janelia.jacs2.model.service.JacsServiceEvent;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class JacsServiceEventJpaDaoTest {

    @Test
    public void getAllEventsByServiceId() {
        EntityManager mockEm = mock(EntityManager.class);
        JacsServiceEventDao testDao = new JacsServiceEventJpaDao(mockEm);
        TypedQuery<JacsServiceEvent> mockQuery = (TypedQuery<JacsServiceEvent>) mock(TypedQuery.class);
        when(mockEm.createQuery(anyString(), same(JacsServiceEvent.class))).thenReturn(mockQuery);
        Long testServiceId = 1L;
        testDao.findAllEventsByServiceId(testServiceId);
        verify(mockEm).createQuery("select e from JacsServiceEvent e where e.jacsServiceData.id = :serviceId order by e.eventTime", JacsServiceEvent.class);
        verify(mockQuery).setParameter("serviceId", testServiceId);
        verify(mockQuery).getResultList();
        verifyNoMoreInteractions(mockQuery);
    }

}
