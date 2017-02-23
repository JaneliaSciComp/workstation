package org.janelia.jacs2.dao;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;

import java.util.List;

public interface DomainObjectDao<T extends DomainObject> extends Dao<T, Number> {
    List<T> findByIds(Subject subject, List<Number> ids);
    <U extends T> List<U> findSubtypesByIds(Subject subject, List<Number> ids, Class<U> entityType);
    PageResult<T> findByOwnerKey(Subject subject, String ownerKey, PageRequest pageRequest);
    boolean lockEntity(String lockKey, T entity);
    boolean unlockEntity(String lockKey, T entity);
}
