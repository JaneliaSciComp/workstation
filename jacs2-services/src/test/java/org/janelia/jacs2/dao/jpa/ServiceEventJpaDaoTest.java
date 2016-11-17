package org.janelia.jacs2.dao.jpa;

import org.janelia.jacs2.dao.ServiceEventDao;
import org.janelia.jacs2.model.service.ServiceEvent;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ServiceEventJpaDaoTest {

    @Test
    public void getAllEventsByServiceId() {
        EntityManager mockEm = mock(EntityManager.class);
        ServiceEventDao testDao = new ServiceEventJpaDao(mockEm);
        TypedQuery<ServiceEvent> mockQuery = (TypedQuery<ServiceEvent>) mock(TypedQuery.class);
        when(mockEm.createQuery(anyString(), same(ServiceEvent.class))).thenReturn(mockQuery);
        Long testServiceId = 1L;
        testDao.findAllEventsByServiceId(testServiceId);
        verify(mockEm).createQuery("select e from ServiceEvent e where e.serviceInfo.id = :serviceId order by e.eventTime", ServiceEvent.class);
        verify(mockQuery).setParameter("serviceId", testServiceId);
        verify(mockQuery).getResultList();
        verifyNoMoreInteractions(mockQuery);
    }

}
