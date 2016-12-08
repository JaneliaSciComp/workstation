package org.janelia.jacs2.dao;

import org.janelia.jacs2.utils.DomainUtils;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

public class DaoFactory {

    @Any @Inject
    private Instance<Dao<?, Number>> daosSource;

    public Dao<?, Number> createDao(String entityName) {
        Class<?> entityClass = DomainUtils.getBasePersistedEntityClass(entityName);
        for (Dao<?, Number> dao : daosSource) {
            Class<?> daoParameter = DomainUtils.getGenericParameterType(dao.getClass(), 0);
            if (entityClass.equals(daoParameter)) {
                return dao;
            }
        }
        throw new IllegalArgumentException("Unknown entity name: " + entityName);
    }

    public DomainObjectDao<?> createDomainObjectDao(String entityName) {
        Class<?> entityClass = DomainUtils.getBasePersistedEntityClass(entityName);
        for (Dao<?, Number> dao : daosSource) {
            Class<?> daoParameter = DomainUtils.getGenericParameterType(dao.getClass(), 0);
            if (entityClass.equals(daoParameter) && dao instanceof DomainObjectDao) {
                return (DomainObjectDao) dao;
            }
        }
        throw new IllegalArgumentException("Unknown or not a domain entity: " + entityName);
    }

}
