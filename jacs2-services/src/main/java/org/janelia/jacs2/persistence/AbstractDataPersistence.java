package org.janelia.jacs2.persistence;

import org.janelia.jacs2.dao.Dao;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.util.function.Consumer;

public class AbstractDataPersistence<D extends Dao<T, I>, T, I> {
    @Named("SLF4J")
    @Inject
    private Logger logger;
    protected Instance<D> daoSource;
    @Inject
    private Instance<UserTransaction> userTransactionSource;


    AbstractDataPersistence(Instance<D> daoSource) {
        this.daoSource = daoSource;
    }

    public void save(T t) {
        execInTransaction(t, e -> {
            D dao = daoSource.get();
            dao.save(e);
        });
    }

    public void update(T t) {
        execInTransaction(t, e -> {
            D dao = daoSource.get();
            dao.update(e);
        });
    }

    void execInTransaction(T t, Consumer<T> consumer) {
        UserTransaction userTransaction = userTransactionSource.get();
        try {
            userTransaction.begin();
            consumer.accept(t);
            userTransaction.commit();
        } catch (Exception e) {
            logger.error("Persist {} error", t, e);
            try {
                if (userTransaction != null) {
                    userTransaction.rollback();
                }
            } catch (SystemException sysExc) {
                logger.error("Transaction rollback error", sysExc);
            }
        }
    }

}
