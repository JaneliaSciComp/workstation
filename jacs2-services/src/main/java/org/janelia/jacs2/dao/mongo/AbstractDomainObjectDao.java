package org.janelia.jacs2.dao.mongo;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.janelia.jacs2.dao.DomainObjectDao;
import org.janelia.jacs2.model.domain.DomainObject;
import org.janelia.jacs2.model.domain.Subject;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.utils.DomainUtils;

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
    public List<T> findByIds(Subject subject, List<Number> ids) {
        if (DomainUtils.isAdminOrUndefined(subject)) {
            return find(Filters.in("_id", ids),
                    null,
                    0,
                    -1,
                    getEntityType());
        } else {
            return find(
                    Filters.and(
                        createSubjectPermissionFilter(subject),
                        Filters.in("_id", ids)
                    ),
                    null,
                    0,
                    -1,
                    getEntityType());
        }
    }

    protected Bson createSubjectPermissionFilter(Subject subject) {
        return Filters.or(
                Filters.eq("ownerKey", subject.getKey()),
                Filters.in("readers", subject.getSubjectClaims())
        );
    }
}
