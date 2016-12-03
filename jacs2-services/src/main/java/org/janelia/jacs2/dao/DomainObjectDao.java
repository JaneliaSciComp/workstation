package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.domain.DomainObject;
import org.janelia.jacs2.model.domain.Subject;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;

import java.util.List;

public interface DomainObjectDao<T extends DomainObject> extends Dao<T, Number> {
    List<T> findByIds(Subject subject, List<Number> ids);
    PageResult<T> findByOwnerKey(Subject subject, String ownerKey, PageRequest pageRequest);
}
