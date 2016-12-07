package org.janelia.jacs2.dao.jpa;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.cdi.qualifier.ComputePersistence;
import org.janelia.jacs2.dao.JacsServiceDataDao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceState;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JPA implementation of JacsServiceDataDao
 */
public class JacsServiceDataJpaDao extends AbstractJpaDao<JacsServiceData, Long> implements JacsServiceDataDao {

    @Inject
    JacsServiceDataJpaDao(@ComputePersistence EntityManager em) {
        super(em);
    }

    @Override
    public List<JacsServiceData> findChildServices(Long serviceId) {
        String query = "select sd from JacsServiceData sd where sd.parentServiceId = :parentServiceId";
        return findByQueryParams(query, ImmutableMap.<String, Object>of("parentServiceId", serviceId), JacsServiceData.class);
    }

    @Override
    public List<JacsServiceData> findServiceHierarchy(Long serviceId) {
        JacsServiceData jacsServiceData = findById(serviceId);
        Preconditions.checkArgument(jacsServiceData != null, "Invalid service ID - no service found for " + serviceId);
        Long rootServiceId = jacsServiceData.getRootServiceId();
        if (rootServiceId == null) {
            rootServiceId = serviceId;
        }
        String query = "select sd from JacsServiceData sd where sd.rootServiceId = :rootServiceId order by id ";
        List<JacsServiceData> fullServiceHierachy = findByQueryParams(query, ImmutableMap.<String, Object>of("rootServiceId", rootServiceId), JacsServiceData.class);
        List<JacsServiceData> serviceHierarchy = new ArrayList<>();
        Set<Long> serviceHierarchySet = new HashSet<>();
        serviceHierarchySet.add(serviceId);
        fullServiceHierachy.stream().forEach(ti -> {
            if (serviceHierarchySet.contains(ti.getParentServiceId())) {
                serviceHierarchy.add(ti);
                serviceHierarchySet.add(ti.getId());
            }
        });
        return serviceHierarchy;
    }

    @Override
    public PageResult<JacsServiceData> findServiceByState(Set<JacsServiceState> requestStates, PageRequest pageRequest) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(requestStates));
        String query = "select sd from JacsServiceData sd where sd.state in :serviceStateValues " + getOrderByStatement(pageRequest.getSortCriteria());
        List<JacsServiceData> results = findByQueryParamsWithPaging(query,
                ImmutableMap.<String, Object>of("serviceStateValues", requestStates),
                pageRequest.getOffset(),
                pageRequest.getPageSize(),
                JacsServiceData.class);
        return new PageResult<>(pageRequest, results);
    }
}
