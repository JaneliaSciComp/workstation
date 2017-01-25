package org.janelia.jacs2.dataservice.persistence;

import org.janelia.jacs2.dao.Dao;

import javax.enterprise.inject.Instance;

public class AbstractDataPersistence<D extends Dao<T, I>, T, I> {
    protected Instance<D> daoSource;

    AbstractDataPersistence(Instance<D> daoSource) {
        this.daoSource = daoSource;
    }

    public T findById(I id) {
        D dao = daoSource.get();
        try {
            return dao.findById(id);
        } finally {
            daoSource.destroy(dao);
        }
    }

    public void save(T t) {
        D dao = daoSource.get();
        try {
            dao.save(t);
        } finally {
            daoSource.destroy(dao);
        }
    }

    public void update(T t) {
        D dao = daoSource.get();
        try {
            dao.update(t);
        } finally {
            daoSource.destroy(dao);
        }
    }
}
