package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.dao.AbstractDao;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.it.jacs.model.domain.support.MongoMapping;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.janelia.jacs2.model.DomainModelUtils;
import org.janelia.jacs2.dao.mongo.utils.TimebasedIdentifierGenerator;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
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
public abstract class AbstractMongoDao<T extends HasIdentifier> extends AbstractDao<T, Number> {

    private final ObjectMapper objectMapper;
    protected final TimebasedIdentifierGenerator idGenerator;
    protected final MongoCollection<T> mongoCollection;

    protected AbstractMongoDao(MongoDatabase mongoDatabase,
                               TimebasedIdentifierGenerator idGenerator,
                               ObjectMapperFactory objectMapperFactory) {
        mongoCollection = mongoDatabase.getCollection(getDomainObjectCollection(), getEntityType());
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapperFactory.newObjectMapper().setSerializationInclusion(JsonInclude.Include.ALWAYS);
    }

    protected String getDomainObjectCollection() {
        Class<T> entityClass = getEntityType();
        MongoMapping mongoMapping = DomainModelUtils.getMapping(entityClass);
        Preconditions.checkArgument(mongoMapping != null, "Entity class " + entityClass.getName() + " is not annotated with MongoMapping");
        return mongoMapping.collectionName();
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

    protected Bson createBsonSortCriteria(List<SortCriteria> sortCriteria) {
        Bson bsonSortCriteria = null;
        if (CollectionUtils.isNotEmpty(sortCriteria)) {
            Map<String, Object> sortCriteriaAsMap = sortCriteria.stream()
                .filter(sc -> StringUtils.isNotBlank(sc.getField()))
                .collect(Collectors.toMap(
                        SortCriteria::getField,
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
            results = results.limit(length);
        }
        return results
                .sort(sortCriteria)
                .into(entityDocs);
    }

    @Override
    public void save(T entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.generateId());
            mongoCollection.insertOne(entity);
        } else {
            upsert(entity);
        }
    }

    public void saveAll(List<T> entities) {
        Iterator<Number> idIterator = idGenerator.generateIdList(entities.size()).iterator();
        List<T> toUpsert = new ArrayList<>();
        List<T> toInsert = new ArrayList<>();
        entities.forEach(e -> {
            if (e.getId() == null) {
                e.setId(idIterator.next());
                toInsert.add(e);
            } else {
                toUpsert.add(e);
            }
        });
        if (!toInsert.isEmpty()) {
            mongoCollection.insertMany(toInsert);
        }
        if (!toUpsert.isEmpty()) {
            UpdateOptions upsertOptions = new UpdateOptions();
            upsertOptions.upsert(true);
            toUpsert.forEach(e -> this.update(e, upsertOptions));
        }
    }

    /**
     * Generic update implementation which updates all first level fields - if their value is set or removes them if the value is null.
     * @param entity to be updated.
     */
    @Override
    public void update(T entity) {
        UpdateOptions updateOptions = new UpdateOptions();
        updateOptions.upsert(false);
        update(entity, updateOptions);
    }

    private void upsert(T entity) {
        UpdateOptions updateOptions = new UpdateOptions();
        updateOptions.upsert(true);
        update(entity, updateOptions);
    }

    private long update(T entity, UpdateOptions updateOptions) {
        return update(entity, getUpdates(entity), updateOptions);
    }

    protected long update(T entity, Bson toUpdate, UpdateOptions updateOptions) {
        UpdateResult result =  mongoCollection.updateOne(getUpdateMatchCriteria(entity), toUpdate, updateOptions);
        return result.getMatchedCount();
    }

    protected Bson getUpdates(T entity) {
        String jsonEntity = null;
        try {
            jsonEntity = objectMapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
        Document bsonEntity = Document.parse(jsonEntity);
        List<Bson> fieldUpdates = bsonEntity.entrySet().stream()
                .map(e -> {
                    Object value = e.getValue();
                    if (value == null) {
                        return Updates.unset(e.getKey());
                    } else {
                        return Updates.set(e.getKey(), e.getValue());
                    }
                }).collect(Collectors.toList());
        return Updates.combine(fieldUpdates);
    }

    protected Bson getUpdateMatchCriteria(T entity) {
        return eq("_id", entity.getId());
    }

    @Override
    public void updateAll(List<T> entities) {
        entities.forEach(this::update);
    }

    @Override
    public void delete(T entity) {
        mongoCollection.deleteOne(eq("_id", entity.getId()));
    }

}
