package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;

/**
 * Data access spec.
 *
 * @param <T> entity type
 * @param <I> entity identifier type
 */
public interface Dao<T, I> {
    T findById(I id);
    void save(T entity);
    void update(T entity);
    void delete (T entity);
    PageResult<T> findAll(PageRequest pageRequest);
    long countAll();

}
