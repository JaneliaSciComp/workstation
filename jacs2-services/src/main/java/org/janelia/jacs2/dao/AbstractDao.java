package org.janelia.jacs2.dao;

import java.lang.reflect.ParameterizedType;

public abstract class AbstractDao<T, I> implements Dao<T, I> {

    @SuppressWarnings("unchecked")
    protected Class<T> getEntityType() {
        Class<T> entityClazz = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        return entityClazz;
    }

}
