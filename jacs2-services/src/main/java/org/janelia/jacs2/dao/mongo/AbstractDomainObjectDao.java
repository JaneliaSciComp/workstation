package org.janelia.jacs2.dao.mongo;

import com.google.common.base.Preconditions;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.dao.DomainObjectDao;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.jacs2.dao.mongo.utils.TimebasedIdentifierGenerator;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.DomainModelUtils;

import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

/**
 * Abstract Domain object DAO.
 *
 * @param <T> type of the element
 */
public abstract class AbstractDomainObjectDao<T extends DomainObject> extends AbstractMongoDao<T> implements DomainObjectDao<T> {

    public AbstractDomainObjectDao(MongoDatabase mongoDatabase, TimebasedIdentifierGenerator idGenerator, ObjectMapperFactory objectMapperFactory) {
        super(mongoDatabase, idGenerator, objectMapperFactory);
    }

    @Override
    public PageResult<T> findByOwnerKey(Subject subject, String ownerKey, PageRequest pageRequest) {
        List<T> results = find(eq("ownerKey", ownerKey),
                createBsonSortCriteria(pageRequest.getSortCriteria()),
                pageRequest.getOffset(),
                pageRequest.getPageSize(),
                getEntityType());
        return new PageResult<>(pageRequest, results);
    }

    @Override
    public <U extends T> List<U> findSubtypesByIds(Subject subject, List<Number> ids, Class<U> entityType) {
        if (DomainModelUtils.isAdminOrUndefined(subject)) {
            return find(Filters.in("_id", ids),
                    null,
                    0,
                    -1,
                    entityType);
        } else {
            return find(
                    Filters.and(
                            createSubjectReadPermissionFilter(subject),
                            Filters.in("_id", ids)
                    ),
                    null,
                    0,
                    -1,
                    entityType);
        }
    }

    @Override
    public boolean lockEntity(String lockKey, T entity) {
        Preconditions.checkArgument(StringUtils.isNotBlank(lockKey));
        Bson lockedEntity = Updates.combine(Updates.set("lockKey", lockKey), Updates.set("lockTimestamp", new Date()));
        entity.setLockKey(lockKey);
        return update(entity, lockedEntity, new UpdateOptions()) > 0;
    }

    @Override
    public boolean unlockEntity(String lockKey, T entity) {
        Bson lockedEntity = Updates.combine(Updates.unset("lockKey"), Updates.unset("lockTimestamp"));
        entity.setLockKey(lockKey);
        return update(entity, lockedEntity, new UpdateOptions()) > 0;
    }

    protected Bson getUpdateMatchCriteria(T entity) {
        Bson lockFilter;
        if (StringUtils.isNotBlank(entity.getLockKey())) {
            lockFilter = Filters.or(Filters.exists("lockKey", false), Filters.eq("lockKey", null), eq("lockKey", entity.getLockKey()));
        } else {
            lockFilter = Filters.or(Filters.exists("lockKey", false), Filters.eq("lockKey", null));
        }
        Bson subjectFilter;
        if (StringUtils.isBlank(entity.getOwnerName())) {
            // either there's no owner key or the writers contain group:all
            subjectFilter = Filters.or(Filters.exists("ownerKey", false), Filters.elemMatch("writers", eq("group:all")));
        } else {
            // use the owner key to check for write permissions
            Subject subject = new Subject();
            subject.setKey(entity.getOwnerKey());
            subjectFilter = createSubjectWritePermissionFilter(subject);
        }
        return Filters.and(
                eq("_id", entity.getId()),
                lockFilter,
                subjectFilter
        );
    }

    @Override
    public List<T> findByIds(Subject subject, List<Number> ids) {
        return findSubtypesByIds(subject, ids, getEntityType());
    }

    protected Bson createSubjectReadPermissionFilter(Subject subject) {
        return Filters.or(
                Filters.eq("ownerKey", subject.getKey()),
                Filters.in("readers", subject.getSubjectClaims())
        );
    }

    protected Bson createSubjectWritePermissionFilter(Subject subject) {
        return Filters.or(
                Filters.eq("ownerKey", subject.getKey()),
                Filters.in("writers", subject.getSubjectClaims())
        );
    }

}
