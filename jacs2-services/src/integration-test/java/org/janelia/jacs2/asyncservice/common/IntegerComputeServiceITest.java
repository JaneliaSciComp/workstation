package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.AbstractITest;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 * Created by murphys on 2/23/17.
 */
public class IntegerComputeServiceITest extends AbstractITest {

    private JacsServiceEngine jacsServiceEngine;
    private ServiceComputationFactory computationFactory;
    private JacsServiceDataPersistence jacsServiceDataPersistence;
    private String defaultWorkingDir;
    private String executablesBaseDir;
    private Instance<ExternalProcessRunner> serviceRunners;
    private Logger logger;

    @Before
    public void setup() {}

    @Test
    public void doTest1() {
        IntegerComputeTestProcessor processor=new IntegerComputeTestProcessor(
                jacsServiceEngine,
                computationFactory,
                jacsServiceDataPersistence,
                defaultWorkingDir,
                executablesBaseDir,
                serviceRunners,
                logger);
        if (logger==null) {
            System.out.println("logger is null");
        } else {
            System.out.println("logger is NOT null");
        }
    }

    @After
    public void tearDown() {}

}
