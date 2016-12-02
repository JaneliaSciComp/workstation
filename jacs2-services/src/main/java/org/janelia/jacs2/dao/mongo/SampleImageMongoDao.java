package org.janelia.jacs2.dao.mongo;

import com.mongodb.client.MongoDatabase;
import org.janelia.jacs2.dao.SampleImageDao;
import org.janelia.jacs2.model.domain.sample.SampleImage;

import javax.inject.Inject;

public class SampleImageMongoDao extends AbstractMongoDao<SampleImage> implements SampleImageDao {
    @Inject
    public SampleImageMongoDao(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }
}
