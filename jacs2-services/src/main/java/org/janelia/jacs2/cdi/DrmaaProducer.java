package org.janelia.jacs2.cdi;

import org.ggf.drmaa.SessionFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class DrmaaProducer {

    @ApplicationScoped
    @Produces
    public SessionFactory createDrmaaSessionFactory() {
        return SessionFactory.getFactory();
    }

}
