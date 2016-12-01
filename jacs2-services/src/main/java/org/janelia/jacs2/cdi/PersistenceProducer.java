package org.janelia.jacs2.cdi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.janelia.jacs2.cdi.qualifier.ApplicationProperties;
import org.janelia.jacs2.cdi.qualifier.ComputePersistence;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.utils.BigIntegerCodec;
import org.janelia.jacs2.utils.DomainCodecProvider;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.io.IOException;
import java.util.Properties;

public class PersistenceProducer {

    @PropertyValue(name = "MongoDB.ConnectionURL")
    @Inject
    private String nmongoConnectionURL;
    @PropertyValue(name = "MongoDB.Database")
    @Inject
    private String mongoDatabase;

    /**
     * Note that there's no disposer for this as we don't want to close it since creating the persistence factory is expensive.
     *
     * @param applicationProperties application properties
     * @return an EntityManagerFactory for the computePersistenceUnit
     * @throws IOException if the configuration properties could not be read.
     */
    @ComputePersistence
    @Produces
    public EntityManagerFactory createEntityManagerFactory(@ApplicationProperties Properties applicationProperties) throws IOException {
        return Persistence.createEntityManagerFactory("ComputePU", applicationProperties);
    }

    public void closeEntityManagerFactory(@Disposes @ComputePersistence EntityManagerFactory emf) {
        if (emf.isOpen()) {
            emf.close();
        }
    }

    @ComputePersistence
    @Produces
    public EntityManager createEntityManager(@ComputePersistence EntityManagerFactory emf) throws IOException {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        return em;
    }

    public void closeEntityManager(@Disposes @ComputePersistence EntityManager em) {
        EntityTransaction tx = em.getTransaction();
        if (tx.isActive()) {
            tx.commit();
        }
        if (em.isOpen()) {
            em.close();
        }
    }

    @Produces
    public MongoClient createMongoClient(ObjectMapper objectMapper) {
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(new DomainCodecProvider(objectMapper)),
                CodecRegistries.fromCodecs(new BigIntegerCodec())
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
