package org.janelia.workstation.integration.spi.domain;

import java.util.ArrayList;
import java.util.Collection;

import org.janelia.model.domain.DomainObject;
import org.openide.util.lookup.Lookups;

/**
 * Delegate class to facilitate dealing with finding compatible acceptors
 * for various tasks.  Convenience class that cuts down on redundant code.
 * 
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ServiceAcceptorHelper {
    
    /**
     * Do a generic lookup, for things that can deal with found values on
     * the outline.
     * 
     * @param T type of the thing to return in collection.
     * @param S type of things to check.
     * @param criterion this is used to judge compatibility of found items.
     * @param clazz tells what type of thing to find.
     * @param path tells the path identifier for searching compatible items.
     * @return a compatible handler for the criterion object.
     */
    public static <S, T extends Compatible<S>> Collection<T> findHandler(S criterion, Class<T> clazz, String path) {
        Collection<? extends T> candidates = Lookups.forPath(path).lookupAll(clazz);
        Collection<T> rtnVal = new ArrayList<>();
        for (T nextAcceptor : candidates) {
            if (nextAcceptor.isCompatible(criterion)) {
                rtnVal.add(nextAcceptor);
            }
        }
        return rtnVal;
    }
    
    public static <S, T extends Compatible<S>> T findFirstHandler(S criterion, Class<T> clazz, String path) {
        Collection<T> rtnVal = findHandler(criterion, clazz, path);
        if (rtnVal.isEmpty()) return null;
        return rtnVal.iterator().next();
    }

    public static Collection<ObjectOpenAcceptor> findAcceptors(Object obj) {
        return findHandler(obj, ObjectOpenAcceptor.class, ObjectOpenAcceptor.LOOKUP_PATH);
    }
    
    public static ObjectOpenAcceptor findFirstAcceptor(Object domainObject) {
        Collection<ObjectOpenAcceptor> handlers = findAcceptors(domainObject);
        if (handlers.isEmpty()) return null;
        return handlers.iterator().next();
    }

    public static Collection<DomainObjectHandler> findHelpers(DomainObject domainObject) {
        return findHandler(domainObject, DomainObjectHandler.class, DomainObjectHandler.DOMAIN_OBJECT_LOOKUP_PATH);
    }
    
    public static DomainObjectHandler findFirstHelper(DomainObject domainObject) {
        Collection<DomainObjectHandler> handlers = findHelpers(domainObject);
        if (handlers.isEmpty()) return null;
        return handlers.iterator().next();
    }
    
}
