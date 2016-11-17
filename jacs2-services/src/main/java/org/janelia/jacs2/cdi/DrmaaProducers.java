package org.janelia.jacs2.cdi;

import org.ggf.drmaa.SessionFactory;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class DrmaaProducers {

    @Singleton
    @Produces
    public SessionFactory createDrmaaSessionFactory() {
        return SessionFactory.getFactory();
    }

}
