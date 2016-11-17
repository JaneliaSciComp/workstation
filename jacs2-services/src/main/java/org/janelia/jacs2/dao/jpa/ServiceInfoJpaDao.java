package org.janelia.jacs2.dao.jpa;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.cdi.ComputePersistence;
import org.janelia.jacs2.dao.ServiceInfoDao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.model.service.ServiceState;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Set;

/**
 * JPA implementation of ServiceInfoDao
 */
public class ServiceInfoJpaDao extends AbstractJpaDao<ServiceInfo, Long> implements ServiceInfoDao {

    @Inject
    ServiceInfoJpaDao(@ComputePersistence EntityManager em) {
        super(em);
    }

    @Override
    public List<ServiceInfo> findChildServices(Long serviceId) {
        String query = "select si from ServiceInfo si where si.parentServiceId = :parentServiceId";
        return findByQueryParams(query, ImmutableMap.<String, Object>of("parentServiceId", serviceId), ServiceInfo.class);
    }

    @Override
    public List<ServiceInfo> findServiceHierarchy(Long serviceId) {
        String query = "select si from ServiceInfo si where si.rootServiceId = :rootServiceId";
        return findByQueryParams(query, ImmutableMap.<String, Object>of("rootServiceId", serviceId), ServiceInfo.class);
    }

    @Override
    public PageResult<ServiceInfo> findServicesByState(Set<ServiceState> requestStates, PageRequest pageRequest) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(requestStates));
        String query = "select si from ServiceInfo si where si.state in :serviceStateValues " + getOrderByStatement(pageRequest.getSortCriteria());
        List<ServiceInfo> results = findByQueryParamsWithPaging(query,
                ImmutableMap.<String, Object>of("serviceStateValues", requestStates),
                pageRequest.getOffset(),
                pageRequest.getPageSize(),
                ServiceInfo.class);
        return new PageResult<>(pageRequest, results);
    }
}
