package org.janelia.jacs2.cdi;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;
import org.janelia.jacs2.utils.BigIntegerCodec;
import org.janelia.jacs2.utils.DomainCodecProvider;
import org.janelia.jacs2.utils.EnumCodec;
import org.janelia.jacs2.utils.MapOfEnumCodec;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class PersistenceProducer {

    @PropertyValue(name = "MongoDB.ConnectionURL")
    @Inject
    private String nmongoConnectionURL;
    @PropertyValue(name = "MongoDB.Database")
    @Inject
    private String mongoDatabase;

    @Singleton
    @Produces
    public MongoClient createMongoClient(ObjectMapperFactory objectMapperFactory) {
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(new DomainCodecProvider(objectMapperFactory)),
                CodecRegistries.fromCodecs(
                        new BigIntegerCodec(),
                        new EnumCodec<>(JacsServiceState.class),
                        new EnumCodec<>(ProcessingLocation.class),
                        new EnumCodec<>(FileType.class),
                        new MapOfEnumCodec<>(FileType.class, HashMap.class),
                        new MapOfEnumCodec<>(FileType.class, LinkedHashMap.class)
                )
        );
        MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder().codecRegistry(codecRegistry);
        MongoClientURI mongoConnectionString = new MongoClientURI(nmongoConnectionURL, optionsBuilder);
        MongoClient mongoClient = new MongoClient(mongoConnectionString);
        return mongoClient;
    }

    @Produces
    public MongoDatabase createMongoDatabase(MongoClient mongoClient) {
        return mongoClient.getDatabase(mongoDatabase);
    }

}
