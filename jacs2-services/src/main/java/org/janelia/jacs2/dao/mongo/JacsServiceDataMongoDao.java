package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.dao.JacsServiceDataDao;
import org.janelia.jacs2.dao.mongo.utils.TimebasedIdentifierGenerator;
import org.janelia.jacs2.model.DataInterval;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lt;

/**
 * Mongo based implementation of JacsServiceDataDao
 */
public class JacsServiceDataMongoDao extends AbstractMongoDao<JacsServiceData> implements JacsServiceDataDao {

    @Inject
    public JacsServiceDataMongoDao(MongoDatabase mongoDatabase, TimebasedIdentifierGenerator idGenerator, ObjectMapperFactory objectMapperFactory) {
        super(mongoDatabase, idGenerator, objectMapperFactory);
    }

    @Override
    public List<JacsServiceData> findChildServices(Number serviceId) {
        return find(eq("parentServiceId", serviceId), null, 0, -1, JacsServiceData.class);
    }

    @Override
    public JacsServiceData findServiceHierarchy(Number serviceId) {
        JacsServiceData jacsServiceData = findById(serviceId);
        if (jacsServiceData == null) {
            return null;
        }
        Number rootServiceId = jacsServiceData.getRootServiceId();
        Map<Number, JacsServiceData> fullServiceHierachy = new LinkedHashMap<>();
        if (rootServiceId == null) {
            rootServiceId = serviceId;
        }
        find(Filters.or(eq("rootServiceId", rootServiceId), eq("_id", rootServiceId)), createBsonSortCriteria(ImmutableList.of(new SortCriteria("_id"))), 0, -1, JacsServiceData.class)
                .forEach(sd -> {
                    fullServiceHierachy.put(sd.getId(), sd);
                });
        fullServiceHierachy.forEach((k, sd) -> {
            JacsServiceData parentService = fullServiceHierachy.get(sd.getParentServiceId());
            if (parentService != null) {
                sd.updateParentService(parentService);
            }
        });
        fullServiceHierachy.forEach((k, sd) -> {
            sd.getDependeciesIds().forEach(id -> sd.addServiceDependency(fullServiceHierachy.get(id)));
        });

        return fullServiceHierachy.get(serviceId);
    }

    @Override
    public PageResult<JacsServiceData> findMatchingServices(JacsServiceData pattern, DataInterval<Date> creationInterval, PageRequest pageRequest) {
        ImmutableList.Builder<Bson> filtersBuilder = new ImmutableList.Builder<>();
        if (pattern.getId() != null) {
            filtersBuilder.add(eq("_id", pattern.getId()));
        }
        if (pattern.getParentServiceId() != null) {
            filtersBuilder.add(eq("parentServiceId", pattern.getParentServiceId()));
        }
        if (pattern.getRootServiceId() != null) {
            filtersBuilder.add(eq("rootServiceId", pattern.getRootServiceId()));
        }
        if (StringUtils.isNotBlank(pattern.getName())) {
            filtersBuilder.add(eq("name", pattern.getName()));
        }
        if (StringUtils.isNotBlank(pattern.getOwner())) {
            filtersBuilder.add(eq("owner", pattern.getOwner()));
        }
        if (creationInterval.hasFrom()) {
            filtersBuilder.add(gte("creationDate", creationInterval.getFrom()));
        }
        if (creationInterval.hasTo()) {
            filtersBuilder.add(lt("creationDate", creationInterval.getTo()));
        }
        ImmutableList<Bson> filters = filtersBuilder.build();

        Bson bsonFilter = null;
        if (!filters.isEmpty()) bsonFilter = and(filters);
        List<JacsServiceData> results = find(bsonFilter, createBsonSortCriteria(pageRequest.getSortCriteria()), pageRequest.getOffset(), pageRequest.getPageSize(), JacsServiceData.class);
        return new PageResult<>(pageRequest, results);
    }

    @Override
    public PageResult<JacsServiceData> findServiceByState(Set<JacsServiceState> requestStates, PageRequest pageRequest) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(requestStates));
        List<JacsServiceData> results = find(in("state", requestStates), createBsonSortCriteria(pageRequest.getSortCriteria()), pageRequest.getOffset(), pageRequest.getPageSize(), JacsServiceData.class);
        return new PageResult<>(pageRequest, results);
    }

    @Override
    public void saveServiceHierarchy(JacsServiceData serviceData) {
        List<JacsServiceData> serviceHierarchy = serviceData.serviceHierarchyStream().map(s -> {
            if (s.getId() == null) {
                s.setId(idGenerator.generateId());
            }
            s.updateParentService(s.getParentService());
            return s;
        }).collect(Collectors.toList());
        serviceHierarchy.stream().forEach(sd -> {
            sd.getDependencies().forEach(dependency -> sd.addServiceDependencyId(dependency));
        });
        saveAll(serviceHierarchy);
    }

    @Override
    public void updateServiceHierarchy(JacsServiceData serviceData) {
        List<JacsServiceData> serviceHierarchy = serviceData.serviceHierarchyStream().collect(Collectors.toList());
        updateAll(serviceHierarchy);
    }

    @Override
    public void update(JacsServiceData entity) {
        entity.setModificationDate(new Date());
        super.update(entity);
    }

}