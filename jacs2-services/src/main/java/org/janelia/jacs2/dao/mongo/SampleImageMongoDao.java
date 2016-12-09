package org.janelia.jacs2.dao.mongo;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.janelia.jacs2.dao.SampleImageDao;
import org.janelia.it.jacs.model.domain.sample.Image;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class SampleImageMongoDao extends AbstractDomainObjectDao<Image> implements SampleImageDao {
    @Inject
    public SampleImageMongoDao(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public void updateImageFiles(Image image) {
        mongoCollection.updateOne(eq("_id", image.getId()), Updates.set("files", image.getFiles()));
    }
}
