package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.domain.DomainObject;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;

public interface DomainObjectDao<T extends DomainObject> extends Dao<T, Number> {
    PageResult<T> findByOwnerKey(String ownerKey, PageRequest pageRequest);
}
