package org.janelia.workstation.integration.spi.domain;

import java.util.ArrayList;
import java.util.Collection;

import org.janelia.model.domain.DomainObject;
import org.openide.util.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegate class to facilitate dealing with finding compatible acceptors
 * for various tasks.  Convenience class that cuts down on redundant code.
 * 
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ServiceAcceptorHelper {

    private static final Logger log = LoggerFactory.getLogger(ServiceAcceptorHelper.class);

    /**
     * Do a generic lookup, for things that can deal with found values on
     * the outline.
     * 
     * @param criterion this is used to judge compatibility of found items.
     * @param clazz tells what type of thing to find.
     * @return a compatible handler for the criterion object.
     */
    public static <S, T extends Compatible<S>> Collection<T> findHandler(S criterion, Class<T> clazz) {
        Collection<? extends T> candidates = Lookup.getDefault().lookupAll(clazz);
        Collection<T> rtnVal = new ArrayList<>();
        log.info("Found {} handlers:", clazz.getSimpleName());
        for (T nextAcceptor : candidates) {
            if (nextAcceptor.isCompatible(criterion)) {
                log.info("  [X] {} is compatible with criterion {}", nextAcceptor.getClass().getSimpleName(), criterion);
                rtnVal.add(nextAcceptor);
            }
            else {
                log.info("  [ ] {} is incompatible with criterion {}", nextAcceptor.getClass().getSimpleName(), criterion);
            }
        }
        return rtnVal;
    }
    
    public static <S, T extends Compatible<S>> T findFirstHandler(S criterion, Class<T> clazz) {
        Collection<T> rtnVal = findHandler(criterion, clazz);
        if (rtnVal.isEmpty()) return null;
        return rtnVal.iterator().next();
    }

    public static Collection<ContextualActionBuilder> findAcceptors(Object obj) {
        return findHandler(obj, ContextualActionBuilder.class);
    }
    
    public static ContextualActionBuilder findFirstAcceptor(Object domainObject) {
        Collection<ContextualActionBuilder> handlers = findAcceptors(domainObject);
        if (handlers.isEmpty()) return null;
        return handlers.iterator().next();
    }

    public static Collection<DomainObjectHandler> findHelpers(DomainObject domainObject) {
        return findHandler(domainObject, DomainObjectHandler.class);
    }
    
    public static DomainObjectHandler findFirstHelper(DomainObject domainObject) {
        Collection<DomainObjectHandler> handlers = findHelpers(domainObject);
        if (handlers.isEmpty()) return null;
        return handlers.iterator().next();
    }
    
}
