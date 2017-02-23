package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.dao.SubjectDao;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.jacs2.dao.mongo.utils.TimebasedIdentifierGenerator;

import javax.inject.Inject;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class SubjectMongoDao extends AbstractMongoDao<Subject> implements SubjectDao {
    @Inject
    public SubjectMongoDao(MongoDatabase mongoDatabase, TimebasedIdentifierGenerator idGenerator, ObjectMapperFactory objectMapperFactory) {
        super(mongoDatabase, idGenerator, objectMapperFactory);
    }

    @Override
    public Subject findByName(String subjectName) {
        List<Subject> entityDocs = find(eq("name", subjectName), null, 0, 1, Subject.class);
        return CollectionUtils.isEmpty(entityDocs) ? null : entityDocs.get(0);
    }
}
