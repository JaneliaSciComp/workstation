package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.domain.DomainObject;
import org.janelia.jacs2.model.domain.HasIdentifier;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;

import java.util.List;

public interface DomainObjectDao<T extends HasIdentifier> extends Dao<T, Number> {
    List<T> findByIds(List<Number> ids);
    PageResult<T> findByOwnerKey(String ownerKey, PageRequest pageRequest);
}
