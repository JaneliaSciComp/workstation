package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.dao.Dao;
import org.janelia.jacs2.dao.DomainObjectDao;
import org.janelia.jacs2.model.domain.DomainObject;
import org.janelia.jacs2.model.domain.HasIdentifier;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.janelia.jacs2.utils.BigIntegerCodec;
import org.janelia.jacs2.utils.DomainCodecProvider;
import org.janelia.jacs2.utils.TimebasedIdentifierGenerator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Spy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIn.isIn;
import static org.junit.Assert.assertThat;

public abstract class AbstractDomainObjectDaoITest<T extends DomainObject> extends AbstractMongoDaoITest<T> {
    protected static final String TEST_OWNER_KEY = "user:test";

    protected void findByOwner(DomainObjectDao<T> dao) {
        String otherOwner = "group:other";
        List<T> testItems = createMultipleTestItems();
        List<T> otherOwnersItems = new ArrayList<>();
        testItems.parallelStream().forEach(s -> {
            if (dataGenerator.nextBoolean()) {
                s.setOwnerKey(otherOwner);
                otherOwnersItems.add(s);
            } else {
                s.setOwnerKey(TEST_OWNER_KEY);
            }
            dao.save(s);
        });
        PageRequest pageRequest = new PageRequest();
        pageRequest.setSortCriteria(ImmutableList.of(
                new SortCriteria("ownerKey", SortDirection.ASC),
                new SortCriteria("creationDate", SortDirection.DESC)));
        PageResult<T> u1Data = dao.findByOwnerKey(TEST_OWNER_KEY, pageRequest);
        PageResult<T> u2Data = dao.findByOwnerKey(otherOwner, pageRequest);
        assertThat(u1Data.getResultList(), everyItem(hasProperty("ownerKey", equalTo(TEST_OWNER_KEY))));
        assertThat(u2Data.getResultList(), everyItem(hasProperty("ownerKey", equalTo(otherOwner))));
        assertThat(u1Data.getResultList(), hasSize(testItems.size() - otherOwnersItems.size()));
        assertThat(u2Data.getResultList(), hasSize(otherOwnersItems.size()));
    }

    protected void findByIds(DomainObjectDao<T> dao) {
        List<T> testItems = createMultipleTestItems();
        testItems.parallelStream().forEach(dao::save);
        List<Number> testItemIds = testItems.stream().map(d -> d.getId()).collect(Collectors.toCollection(ArrayList<Number>::new));
        List<T> res = dao.findByIds(testItemIds);
        assertThat(res, hasSize(testItems.size()));
        assertThat(res, everyItem(hasProperty("id", isIn(testItemIds))));
    }

}
