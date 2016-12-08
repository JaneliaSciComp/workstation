package org.janelia.jacs2.dao.mongo;

import com.mongodb.client.MongoDatabase;
import org.janelia.jacs2.dao.SampleImageDao;
import org.janelia.it.jacs.model.domain.sample.Image;

import javax.inject.Inject;

public class SampleImageMongoDao extends AbstractDomainObjectDao<Image> implements SampleImageDao {
    @Inject
    public SampleImageMongoDao(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }
}
