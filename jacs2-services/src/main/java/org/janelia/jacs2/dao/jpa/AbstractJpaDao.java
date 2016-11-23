package org.janelia.jacs2.dao.jpa;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.dao.Dao;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;

import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base JPA implementation for a DAO
 */
public abstract class AbstractJpaDao<T, I> implements Dao<T, I> {

    private EntityManager entityManager;

    AbstractJpaDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    private Class<T> getEntityType() {
        Class<T> entityClazz = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        return entityClazz;
    }

    @Override
    public T findById(I id) {
        return findById(id, getEntityType());
    }

    private T findById(I id, Class<T> resultType) {
        return entityManager.find(resultType, id);
    }

    @Override
    public void save(T entity) {
        entityManager.persist(entity);
    }

    @Override
    public void update(T entity) {
        entityManager.merge(entity);
    }

    @Override
    public void delete(T entity) {
        if (entity != null) entityManager.remove(entity);
    }

    @Override
    public PageResult<T> findAll(PageRequest pageRequest) {
        Class<T> entityType = getEntityType();
        String orderByStmt = getOrderByStatement(pageRequest.getSortCriteria());
        String query = String.format("select e from %s e %s", entityType.getSimpleName(), orderByStmt);
        List<T> results = findByQueryParamsWithPaging(query,
                ImmutableMap.<String, Object>of(),
                pageRequest.getOffset(),
                pageRequest.getPageSize(),
                entityType);
        return new PageResult<>(pageRequest, results);
    }

    protected String getOrderByStatement(List<SortCriteria> sortCriteria) {
        String orderByStmt = "";
        if (CollectionUtils.isNotEmpty(sortCriteria)) {
            Optional<String> fields = sortCriteria.stream()
                    .filter(sc -> StringUtils.isNotBlank(sc.getField()))
                    .map(sc -> sc.getField() + " " + sc.getDirection().toString())
                    .reduce((a,b) -> a + "," + b);
            if (fields.isPresent() && StringUtils.isNotBlank(fields.get())) {
                orderByStmt = "order by " + fields.get();
            }
        }
        return orderByStmt;
    }

    protected  <R> List<R> findByQueryParams(String queryString, Map<String, Object> queryParams, Class<R> resultType) {
        TypedQuery<R> query = prepareQuery(queryString, queryParams, resultType);
        return query.getResultList();
    }

    protected  <R> List<R> findByQueryParamsWithPaging(String queryString, Map<String, Object> queryParams, long offset, int length, Class<R> resultType) {
        TypedQuery<R> query = prepareQuery(queryString, queryParams, resultType);
        if (offset > 0L) query.setFirstResult((int) offset);
        if (length > 0) query.setMaxResults(length);
        return query.getResultList();
    }

    protected <R> R getAtMostOneResult(String queryString, Map<String, Object> queryParams, Class<R> resultType) {
        TypedQuery<R> query = prepareQuery(queryString, queryParams, resultType).setMaxResults(2);
        List<R> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new NonUniqueResultException();
        }
    }

    protected <R> R getSingleResult(String queryString, Map<String, Object> queryParams, Class<R> resultType) {
        return prepareQuery(queryString, queryParams, resultType).getSingleResult();
    }

    protected <R> TypedQuery<R> prepareQuery(String queryString, Map<String, Object> queryParams, Class<R> resultType) {
        TypedQuery<R> query = entityManager.createQuery(queryString, resultType);
        setQueryParameters(query, queryParams);
        return query;
    }

    protected void setQueryParameters(Query query, Map<String, Object> queryParams) {
        for (Map.Entry<String, Object> paramEntry : queryParams.entrySet()) {
            query.setParameter(paramEntry.getKey(), paramEntry.getValue());
        }
    }

    @Override
    public long countAll() {
        Class<T> entityType = getEntityType();
        String query = String.format("select count(e) from %s e", entityType.getSimpleName());
        return getSingleResult(query, ImmutableMap.<String, Object>of(), Long.class);
    }

}
