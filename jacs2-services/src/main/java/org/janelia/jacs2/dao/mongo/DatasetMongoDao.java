package org.janelia.jacs2.dao.mongo;

import com.mongodb.client.MongoDatabase;
import org.janelia.jacs2.dao.DatasetDao;
import org.janelia.jacs2.model.domain.sample.DataSet;

import javax.inject.Inject;

public class DatasetMongoDao extends AbstractDomainObjectDao<DataSet> implements DatasetDao {
    @Inject
    public DatasetMongoDao(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }
}
