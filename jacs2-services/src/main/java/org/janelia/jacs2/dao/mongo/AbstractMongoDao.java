package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.janelia.jacs2.dao.AbstractDao;
import org.janelia.jacs2.dao.DomainObjectDao;
import org.janelia.jacs2.model.domain.DomainObject;
import org.janelia.jacs2.model.domain.annotations.MongoMapping;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.janelia.jacs2.utils.TimebasedIdentifierGenerator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * Abstract Mongo DAO.
 *
 * @param <T> type of the element
 */
public abstract class AbstractMongoDao<T extends DomainObject> extends AbstractDao<T, Number> implements DomainObjectDao<T> {

    @Inject
    protected ObjectMapper objectMapper;
    @Inject
    protected TimebasedIdentifierGenerator idGenerator;
    protected MongoCollection<T> mongoCollection;

    protected AbstractMongoDao(MongoDatabase mongoDatabase) {
        mongoCollection = mongoDatabase.getCollection(getDomainObjectCollection(), getEntityType());
    }

    protected String getDomainObjectCollection() {
        Class<T> entityClass = getEntityType();
        MongoMapping mongoMapping = getMapping(entityClass);
        Preconditions.checkArgument(mongoMapping != null, "Entity class " + entityClass.getName() + "is not annotated with MongoMapping");
        return mongoMapping.collectionName();
    }

    protected <D> MongoMapping getMapping(Class<D> objectClass) {
        MongoMapping mongoMapping = null;
        for(Class<?> clazz = objectClass; clazz != null; clazz = clazz.getSuperclass()) {
            if (clazz.isAnnotationPresent(MongoMapping.class)) {
                mongoMapping = clazz.getAnnotation(MongoMapping.class);
                break;
            }
        }
        return mongoMapping;
    }

    @Override
    public T findById(Number id) {
        List<T> entityDocs = find(eq("_id", id), null, 0, 1, getEntityType());
        return CollectionUtils.isEmpty(entityDocs) ? null : entityDocs.get(0);
    }


    @Override
    public PageResult<T> findAll(PageRequest pageRequest) {
        List<T> results = find(null,
                createBsonSortCriteria(pageRequest.getSortCriteria()),
                pageRequest.getOffset(),
                pageRequest.getPageSize(),
                getEntityType());
        return new PageResult<>(pageRequest, results);
    }

    private Bson createBsonSortCriteria(List<SortCriteria> sortCriteria) {
        Bson bsonSortCriteria = null;
        if (CollectionUtils.isNotEmpty(sortCriteria)) {
            Map<String, Object> sortCriteriaAsMap = sortCriteria.stream()
                .filter(sc -> StringUtils.isNotBlank(sc.getField()))
                .collect(Collectors.toMap(
                        sc -> sc.getField(),
                        sc -> sc.getDirection() == SortDirection.DESC ? -1 : 1,
                        (sc1, sc2) -> sc2,
                        LinkedHashMap::new));
            bsonSortCriteria = new Document(sortCriteriaAsMap);
        }
        return bsonSortCriteria;
    }

    @Override
    public long countAll() {
        return mongoCollection.count();
    }

    protected <R> List<R> find(Bson queryFilter, Bson sortCriteria, long offset, int length, Class<R> resultType) {
        List<R> entityDocs = new ArrayList<>();
        FindIterable<R> results = mongoCollection.find(resultType);
        if (queryFilter != null) {
            results = results.filter(queryFilter);
        }
        if (offset > 0) {
            results = results.skip((int) offset);
        }
        if (length > 0) {
            results = results.limit((int) length);
        }
        return results
                .sort(sortCriteria)
                .into(entityDocs);
    }

    @Override
    public void save(T entity) {
        entity.setId(idGenerator.generateId());
        mongoCollection.insertOne(entity);
    }

    /**
     * Generic update implementation which updates all first level fields - if their value is set or removes them if the value is null.
     * @param entity to be updated.
     */
    @Override
    public void update(T entity) {
        try {
            String jsonEntity = objectMapper.writeValueAsString(entity);
            Document bsonEntity = Document.parse(jsonEntity);
            List<Bson> fieldUpdates = bsonEntity.entrySet().stream().map(e -> {
                Object value = e.getValue();
                if (value == null) {
                    return Updates.unset(e.getKey());
                } else {
                    return Updates.set(e.getKey(), e.getValue());
                }
            }).collect(Collectors.toList());
            Bson toUpdate = Updates.combine(fieldUpdates);
            mongoCollection.updateOne(eq("_id", entity.getId()), toUpdate);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void delete(T entity) {
        mongoCollection.deleteOne(eq("_id", entity.getId()));
    }

    @Override
    public PageResult<T> findByOwnerKey(String ownerKey, PageRequest pageRequest) {
        List<T> results = find(eq("ownerKey", ownerKey),
                createBsonSortCriteria(pageRequest.getSortCriteria()),
                pageRequest.getOffset(),
                pageRequest.getPageSize(),
                getEntityType());
        return new PageResult<>(pageRequest, results);
    }

    @Override
    public List<T> findByIds(List<Number> ids) {
        return find(Filters.in("_id", ids),
                null,
                0,
                -1,
                getEntityType());
    }
}
