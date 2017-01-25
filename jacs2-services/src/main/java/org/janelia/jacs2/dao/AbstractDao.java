package org.janelia.jacs2.dao;

import org.janelia.jacs2.model.DomainModelUtils;

public abstract class AbstractDao<T, I> implements Dao<T, I> {

    protected Class<T> getEntityType() {
        return DomainModelUtils.getGenericParameterType(this.getClass(), 0);
    }

}
