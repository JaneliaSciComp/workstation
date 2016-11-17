package org.janelia.jacs2.dao.jpa;

import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.cdi.ComputePersistence;
import org.janelia.jacs2.dao.ServiceEventDao;
import org.janelia.jacs2.model.service.ServiceEvent;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * JPA implementation of ServiceEventDao
 */
public class ServiceEventJpaDao extends AbstractJpaDao<ServiceEvent, Long> implements ServiceEventDao {

    @Inject
    ServiceEventJpaDao(@ComputePersistence EntityManager em) {
        super(em);
    }

    @Override
    public List<ServiceEvent> findAllEventsByServiceId(Long serviceId) {
        String query = "select e from ServiceEvent e where e.serviceInfo.id = :serviceId order by e.eventTime";
        return findByQueryParams(query, ImmutableMap.<String, Object>of("serviceId", serviceId), ServiceEvent.class);
    }
}
