package org.janelia.jacs2;

import org.janelia.jacs2.cdi.ApplicationPropertiesProvider;
import org.junit.BeforeClass;

import java.util.Properties;

public abstract class AbstractITest {
    protected static Properties integrationTestsConfig;

    @BeforeClass
    public static void setUpTestsConfig() {
        integrationTestsConfig = new ApplicationPropertiesProvider()
                .fromFile("build/resources/integrationTest/jacs_test.properties")
                .fromEnvVar("JACS2_CONFIG_TEST")
                .build();
    }

}
