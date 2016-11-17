package org.janelia.jacs2.persistence;

import org.janelia.jacs2.dao.Dao;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.util.function.Consumer;

public class AbstractDataPersistence<D extends Dao<T, I>, T, I> {
    @Named("SLF4J")
    @Inject
    private Logger logger;
    protected D dao;
    @Inject
    private UserTransaction userTransaction;


    AbstractDataPersistence(D dao) {
        this.dao = dao;
    }

    public void save(T t) {
        execInTransaction(t, e -> {
            dao.save(e);
        });
    }

    public void update(T t) {
        execInTransaction(t, e -> {
            dao.update(e);
        });
    }

    void execInTransaction(T t, Consumer<T> consumer) {
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
