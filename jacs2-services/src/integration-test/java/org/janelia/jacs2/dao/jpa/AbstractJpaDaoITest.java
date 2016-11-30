package org.janelia.jacs2.dao.jpa;

import org.junit.Before;
import org.junit.BeforeClass;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AbstractJpaDaoITest {
    private static EntityManagerFactory testEntityManagerFactory;

    protected EntityManager testEntityManager;

    @BeforeClass
    public static void setUpPersistenceFactory() throws IOException {
        Properties persistenceConfig = new Properties();
        try (InputStream configReader = new FileInputStream("build/resources/integrationTest/jacs_test.properties")) {
            persistenceConfig.load(configReader);
        }
        testEntityManagerFactory = Persistence.createEntityManagerFactory("ComputePU", persistenceConfig);
    }

    @Before
    public final void setUpPersistenceContext() {
        testEntityManager = testEntityManagerFactory.createEntityManager();
    }

}
