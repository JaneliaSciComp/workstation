package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.dao.DatasetDao;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.jacs2.dao.mongo.utils.TimebasedIdentifierGenerator;

import javax.inject.Inject;

public class DatasetMongoDao extends AbstractDomainObjectDao<DataSet> implements DatasetDao {
    @Inject
    public DatasetMongoDao(MongoDatabase mongoDatabase, TimebasedIdentifierGenerator idGenerator, ObjectMapperFactory objectMapperFactory) {
        super(mongoDatabase, idGenerator, objectMapperFactory);
    }
}
