package org.janelia.jacs2.cdi;

import org.janelia.jacs2.cdi.qualifier.ApplicationProperties;
import org.janelia.jacs2.cdi.qualifier.ComputePersistence;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PersistenceProducer {

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
}
