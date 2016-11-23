package org.janelia.jacs2.app;

import org.glassfish.jersey.server.ResourceConfig;

public class JAXAppConfig extends ResourceConfig {
    public JAXAppConfig() {
        packages(true,
                "org.janelia.jacs2.rest",
                "org.janelia.jacs2.job",
                "org.janelia.jacs2.provider");
    }
}
