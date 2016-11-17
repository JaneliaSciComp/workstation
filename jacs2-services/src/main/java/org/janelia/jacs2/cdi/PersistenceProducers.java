package org.janelia.jacs2.cdi;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PersistenceProducers {

    /**
     * Note that there's no disposer for this as we don't want to close it since creating the persistence factory is expensive.
     *
     * @return an EntityManagerFactory for the computePersistenceUnit
     * @throws IOException if the configuration properties could not be read.
     */
    @Singleton
    @ComputePersistence
    @Produces
    public EntityManagerFactory createEntityManagerFactory() throws IOException {
        Properties persistenceConfig = new Properties();
        try (InputStream configReader = PersistenceProducers.class.getResourceAsStream("/compute.properties")) {
            persistenceConfig.load(configReader);
        }
        return Persistence.createEntityManagerFactory("ComputePU", persistenceConfig);
    }

    public void closeEntityManagerFactory(@Disposes @ComputePersistence EntityManagerFactory emf) {
        if (emf.isOpen()) {
            emf.close();
        }
    }

    @ComputePersistence
    @Produces
    public EntityManager createEntityManager(@ComputePersistence EntityManagerFactory emf) throws IOException {
        return emf.createEntityManager();
    }

    public void closeEntityManager(@Disposes @ComputePersistence EntityManager em) {
        if (em.isOpen()) {
            em.close();
        }
    }
}
