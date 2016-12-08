package org.janelia.jacs2.dao.jpa;

import org.janelia.jacs2.AbstractITest;
import org.junit.Before;
import org.junit.BeforeClass;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class AbstractJpaDaoITest extends AbstractITest {
    private static EntityManagerFactory testEntityManagerFactory;

    protected EntityManager testEntityManager;

    @BeforeClass
    public static void setUpPersistenceFactory() throws IOException {
        testEntityManagerFactory = Persistence.createEntityManagerFactory("ComputePU", integrationTestsConfig);
    }

    @Before
    public final void setUpPersistenceContext() {
        testEntityManager = testEntityManagerFactory.createEntityManager();
    }

}
