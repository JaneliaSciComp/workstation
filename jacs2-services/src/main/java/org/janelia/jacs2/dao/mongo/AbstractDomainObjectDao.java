package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.janelia.jacs2.dao.AbstractDao;
import org.janelia.jacs2.dao.DomainObjectDao;
import org.janelia.jacs2.model.domain.DomainObject;
import org.janelia.jacs2.model.domain.HasIdentifier;
import org.janelia.jacs2.model.domain.annotations.MongoMapping;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.janelia.jacs2.utils.TimebasedIdentifierGenerator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public PageResult<T> findByOwnerKey(String ownerKey, PageRequest pageRequest) {
        List<T> results = find(eq("ownerKey", ownerKey),
                createBsonSortCriteria(pageRequest.getSortCriteria()),
                pageRequest.getOffset(),
                pageRequest.getPageSize(),
                getEntityType());
        return new PageResult<>(pageRequest, results);
    }

    @Override
    public List<T> findByIds(List<Number> ids) {
        return find(Filters.in("_id", ids),
                null,
                0,
                -1,
                getEntityType());
    }
}
