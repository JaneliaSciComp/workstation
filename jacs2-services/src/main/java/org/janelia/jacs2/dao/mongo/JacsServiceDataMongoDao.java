package org.janelia.jacs2.dao.mongo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.dao.JacsServiceDataDao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceState;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

/**
 * Mongo based implementation of JacsServiceDataDao
 */
public class JacsServiceDataMongoDao extends AbstractMongoDao<JacsServiceData> implements JacsServiceDataDao {

    @Inject
    public JacsServiceDataMongoDao(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public List<JacsServiceData> findChildServices(Number serviceId) {
        return find(eq("parentServiceId", serviceId), null, 0, -1, JacsServiceData.class);
    }

    @Override
    public List<JacsServiceData> findServiceHierarchy(Number serviceId) {
        JacsServiceData jacsServiceData = findById(serviceId);
        Preconditions.checkArgument(jacsServiceData != null, "Invalid service ID - no service found for " + serviceId);
        Number rootServiceId = jacsServiceData.getRootServiceId();
        if (rootServiceId == null) {
            rootServiceId = serviceId;
        }
        List<JacsServiceData> fullServiceHierachy = find(eq("rootServiceId", rootServiceId), createBsonSortCriteria(ImmutableList.of(new SortCriteria("_id"))), 0, -1, JacsServiceData.class);
        List<JacsServiceData> serviceHierarchy = new ArrayList<>();
        Set<Number> serviceHierarchySet = new HashSet<>();
        serviceHierarchy.add(jacsServiceData);
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
    public PageResult<JacsServiceData> findMatchingServices(JacsServiceData pattern, Date from, Date to, PageRequest pageRequest) {
        // !!!!!!!!!!!!!!!!! TODO
        return null;
    }

    @Override
    public PageResult<JacsServiceData> findServiceByState(Set<JacsServiceState> requestStates, PageRequest pageRequest) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(requestStates));
        List<JacsServiceData> results = find(in("state", requestStates), createBsonSortCriteria(pageRequest.getSortCriteria()), pageRequest.getOffset(), pageRequest.getPageSize(), JacsServiceData.class);
        return new PageResult<>(pageRequest, results);
    }
}
