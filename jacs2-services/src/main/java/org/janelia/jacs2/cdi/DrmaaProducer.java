package org.janelia.jacs2.cdi;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class DrmaaProducer {

    @ApplicationScoped
    @Produces
    public SessionFactory createDrmaaSessionFactory() {
        return SessionFactory.getFactory();
    }

    @ApplicationScoped
    @Produces
    public Session createDrmaaSession(SessionFactory drmaaSessionFactory) throws DrmaaException {
        Session drmaaSession = drmaaSessionFactory.getSession();
        drmaaSession.init(null);
        return drmaaSession;
    }

    public void closeDrmaaSession(@Disposes @Default Session drmaaSession) throws DrmaaException {
        drmaaSession.exit();
    }
}
