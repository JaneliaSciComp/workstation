package org.janelia.jacs2.dao;

import org.janelia.jacs2.utils.DomainUtils;

import java.lang.reflect.ParameterizedType;

public abstract class AbstractDao<T, I> implements Dao<T, I> {

    protected Class<T> getEntityType() {
        return DomainUtils.getGenericParameterType(this.getClass(), 0);
    }

}
