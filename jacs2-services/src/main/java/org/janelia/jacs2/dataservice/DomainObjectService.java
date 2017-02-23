package org.janelia.jacs2.dataservice;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.jacs2.dao.DaoFactory;
import org.janelia.jacs2.dao.DomainObjectDao;

import javax.inject.Inject;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class DomainObjectService implements LockService {

    private static final SecureRandom LOCK_KEY_GENERATOR = new SecureRandom();

    private final DaoFactory daoFactory;

    @Inject
    public DomainObjectService(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    public Stream<DomainObject> streamAllReferences(Subject subject, Stream<Reference> refStream) {
        return refStream.collect(
                ArrayListMultimap::<String, Number>create,
                (m, ref) -> m.put(ref.getTargetClassname(), ref.getTargetId()),
                (m1, m2) -> m1.putAll(m2)).asMap().entrySet().stream().flatMap(e -> {
                    DomainObjectDao<?> dao = daoFactory.createDomainObjectDao(e.getKey());
                    return dao.findByIds(subject, ImmutableList.copyOf(e.getValue())).stream();
                });
    }

    @Override
    public <T extends DomainObject> String lock(T entity, long timeout, TimeUnit timeunit) {
        long startTimeMillis = System.currentTimeMillis();
        DomainObjectDao<T> dao = (DomainObjectDao<T>) daoFactory.createDomainObjectDao(entity.getEntityName());
        String lockKey = nextKey();
        for (;;) {
            String lock = tryLock(entity, lockKey, dao);
            if (lock != null) {
                return lock;
            }
            if (timeout > 0) {
                long nowInMillis = System.currentTimeMillis();
                if (nowInMillis - startTimeMillis > timeunit.toMillis(timeout)) {
                    break;
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }
        return null;
    }

    @Override
    public <T extends DomainObject> String tryLock(T entity) {
        DomainObjectDao<T> dao = (DomainObjectDao<T>) daoFactory.createDomainObjectDao(entity.getEntityName());
        String lockKey = nextKey();
        return tryLock(entity, lockKey, dao);
    }

    private <T extends DomainObject> String tryLock(T entity, String lockKey, DomainObjectDao<T> dao) {
        if (dao.lockEntity(lockKey, entity)) {
            return lockKey;
        } else {
            return null;
        }
    }

    @Override
    public <T extends DomainObject> boolean isLocked(T entity) {
        DomainObjectDao<T> dao = (DomainObjectDao<T>) daoFactory.createDomainObjectDao(entity.getEntityName());
        T existingEntity = dao.findById(entity.getId());
        if (existingEntity == null) {
            throw new IllegalArgumentException("Entity not found");
        }
        return StringUtils.isNotBlank(existingEntity.getOwnerKey());
    }

    @Override
    public <T extends DomainObject> boolean unlock(String lockKey, T entity) {
        DomainObjectDao<T> dao = (DomainObjectDao<T>) daoFactory.createDomainObjectDao(entity.getEntityName());
        return dao.unlockEntity(lockKey, entity);
    }

    private String nextKey() {
        return new BigInteger(130, LOCK_KEY_GENERATOR).toString(32);
    }
}
