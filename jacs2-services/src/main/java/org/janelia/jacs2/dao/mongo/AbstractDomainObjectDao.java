package org.janelia.jacs2.dao.mongo;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.janelia.jacs2.dao.DomainObjectDao;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.DomainModelUtils;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;

/**
 * Abstract Domain object DAO.
 *
 * @param <T> type of the element
 */
public abstract class AbstractDomainObjectDao<T extends DomainObject> extends AbstractMongoDao<T> implements DomainObjectDao<T> {

    protected AbstractDomainObjectDao(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
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
                            createSubjectPermissionFilter(subject),
                            Filters.in("_id", ids)
                    ),
                    null,
                    0,
                    -1,
                    entityType);
        }
    }

    @Override
    public List<T> findByIds(Subject subject, List<Number> ids) {
        return findSubtypesByIds(subject, ids, getEntityType());
    }

    protected Bson createSubjectPermissionFilter(Subject subject) {
        return Filters.or(
                Filters.eq("ownerKey", subject.getKey()),
                Filters.in("readers", subject.getSubjectClaims())
        );
    }
}
