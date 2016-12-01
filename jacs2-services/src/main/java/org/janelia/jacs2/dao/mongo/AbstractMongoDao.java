package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.janelia.jacs2.dao.AbstractDao;
import org.janelia.jacs2.model.domain.DomainObject;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.janelia.jacs2.utils.TimebasedIdentifierGenerator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * Abstract Mongo DAO.
 *
 * @param <T> type of the element
 */
public abstract class AbstractMongoDao<T extends DomainObject> extends AbstractDao<T, Number> {

    @Inject
    protected ObjectMapper objectMapper;
    @Inject
    protected TimebasedIdentifierGenerator idGenerator;
    protected MongoCollection<T> mongoCollection;

    protected AbstractMongoDao(MongoDatabase mongoDatabase, String collectionName) {
        mongoCollection = mongoDatabase.getCollection(collectionName, getEntityType());
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
                .collect(Collectors.toMap(sc -> sc.getField(), sc -> sc.getDirection() == SortDirection.DESC ? -1 : 1));
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
        return mongoCollection.find(resultType)
                .filter(queryFilter)
                .skip((int) offset)
                .limit(length)
                .sort(sortCriteria)
                .into(entityDocs);
    }

    @Override
    public void save(T entity) {
        entity.setId(idGenerator.generateId());
        mongoCollection.insertOne(entity);
    }

    @Override
    public void update(T entity) {
        try {
            String jsonEntity = objectMapper.writeValueAsString(entity);
            Document bsonEntity = Document.parse(jsonEntity);
            mongoCollection.updateOne(eq("_id", entity.getId()), bsonEntity);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void delete(T entity) {
        mongoCollection.deleteOne(eq("_id", entity.getId()));
    }
}
