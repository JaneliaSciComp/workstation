package org.janelia.jacs2.dao.jpa;

import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.cdi.qualifier.ComputePersistence;
import org.janelia.jacs2.dao.JacsServiceEventDao;
import org.janelia.jacs2.model.service.JacsServiceEvent;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * JPA implementation of JacsServiceEventJpaDao
 */
public class JacsServiceEventJpaDao extends AbstractJpaDao<JacsServiceEvent, Long> implements JacsServiceEventDao {

    @Inject
    JacsServiceEventJpaDao(@ComputePersistence EntityManager em) {
        super(em);
    }

    @Override
    public List<JacsServiceEvent> findAllEventsByServiceId(Long serviceId) {
        String query = "select e from JacsServiceEvent e where e.jacsServiceData.id = :serviceId order by e.eventTime";
        return findByQueryParams(query, ImmutableMap.<String, Object>of("serviceId", serviceId), JacsServiceEvent.class);
    }
}
