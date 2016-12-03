package org.janelia.jacs2.dao.mongo;

import com.mongodb.client.MongoDatabase;
import org.janelia.jacs2.dao.SampleDao;
import org.janelia.jacs2.model.domain.sample.Sample;

import javax.inject.Inject;
import java.math.BigInteger;

public class SampleMongoDao extends AbstractDomainObjectDao<Sample> implements SampleDao {
    @Inject
    public SampleMongoDao(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }
}
