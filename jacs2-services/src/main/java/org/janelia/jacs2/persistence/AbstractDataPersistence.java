package org.janelia.jacs2.persistence;

import org.janelia.jacs2.dao.Dao;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

public class AbstractDataPersistence<D extends Dao<T, I>, T, I> {
    @Named("SLF4J")
    @Inject
    private Logger logger;
    protected Instance<D> daoSource;

    AbstractDataPersistence(Instance<D> daoSource) {
        this.daoSource = daoSource;
    }

    public void save(T t) {
        D dao = daoSource.get();
        try {
            logger.debug("Save {}", t);
            dao.save(t);
        } finally {
            daoSource.destroy(dao);
        }
    }

    public void update(T t) {
        D dao = daoSource.get();
        try {
            logger.debug("Update {}", t);
            dao.update(t);
        } finally {
            daoSource.destroy(dao);
        }
    }
}
