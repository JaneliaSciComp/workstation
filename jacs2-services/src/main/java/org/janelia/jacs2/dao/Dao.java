package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;

import java.util.List;

/**
 * Data access spec.
 *
 * @param <T> entity type
 * @param <I> entity identifier type
 */
public interface Dao<T, I> {
    T findById(I id);
    void save(T entity);
    void saveAll(List<T> entities);
    void update(T entity);
    void updateAll(List<T> entities);
    void delete (T entity);
    PageResult<T> findAll(PageRequest pageRequest);
    long countAll();

}
