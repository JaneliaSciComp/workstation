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
import org.janelia.jacs2.model.domain.sample.Sample;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.janelia.jacs2.utils.BigIntegerCodec;
import org.janelia.jacs2.utils.DomainCodecProvider;
import org.janelia.jacs2.utils.TimebasedIdentifierGenerator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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

public abstract class AbstractMongoDaoITest<T extends HasIdentifier> {
    protected static final String TEST_OWNER_KEY = "user:test";

    private static MongoClient testMongoClient;
    private static ObjectMapper testObjectMapper = ObjectMapperFactory.instance().getObjectMapper();

    private static Properties testConfig;

    protected MongoDatabase testMongoDatabase;
    @Spy
    protected ObjectMapper objectMapper = ObjectMapperFactory.instance().getObjectMapper();
    @Spy
    protected TimebasedIdentifierGenerator idGenerator = new TimebasedIdentifierGenerator(0);
    protected Random dataGenerator = new Random();

    @BeforeClass
    public static void setUpMongoClient() throws IOException {
        testConfig = new Properties();
        try (InputStream configReader = new FileInputStream("build/resources/integrationTest/jacs_test.properties")) {
            testConfig.load(configReader);
        }
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(new DomainCodecProvider(testObjectMapper)),
                CodecRegistries.fromCodecs(new BigIntegerCodec())
        );
        MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder().codecRegistry(codecRegistry).maxConnectionIdleTime(60000);
        MongoClientURI mongoConnectionString = new MongoClientURI(testConfig.getProperty("MongoDB.ConnectionURL"), optionsBuilder);
        testMongoClient = new MongoClient(mongoConnectionString);
    }

    @Before
    public final void setUpMongoDatabase() {
        testMongoDatabase = testMongoClient.getDatabase(testConfig.getProperty("MongoDB.Database"));
    }

    protected void deleteAll(Dao<T, Number> dao, List<T> es) {
        for (T e : es) {
            delete(dao, e);
        }
    }

    protected void delete(Dao<T, Number> dao, T e) {
        if (e.getId() != null) {
            dao.delete(e);
        }
    }

    protected <R extends DomainObject> void findByOwner(DomainObjectDao<R> dao) {
        String otherOwner = "group:other";
        List<R> testItems = createMultipleTestItems();
        List<R> otherOwnersItems = new ArrayList<>();
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
        PageResult<R> u1Data = dao.findByOwnerKey(TEST_OWNER_KEY, pageRequest);
        PageResult<R> u2Data = dao.findByOwnerKey(otherOwner, pageRequest);
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

    protected abstract <R> List<R> createMultipleTestItems();

}
