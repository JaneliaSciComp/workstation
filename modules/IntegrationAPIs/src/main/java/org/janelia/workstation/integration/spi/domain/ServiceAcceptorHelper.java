package org.janelia.workstation.integration.spi.domain;

import org.janelia.model.domain.DomainObject;
import org.openide.util.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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
     * @return a compatible handler for the criterion object.
     */
    public static Collection<DomainObjectHandler> findHandler(
            Class<? extends DomainObject> criterion) {

        if (criterion==null) return Collections.emptyList();
        Collection<? extends DomainObjectHandler> candidates = Lookup.getDefault().lookupAll(DomainObjectHandler.class);
        Collection<DomainObjectHandler> rtnVal = new ArrayList<>();
        log.trace("Found DomainObjectHandler handlers:");
        for (DomainObjectHandler nextAcceptor : candidates) {
            if (nextAcceptor.isCompatible(criterion)) {
                log.trace("  [X] {} is compatible with criterion {}", nextAcceptor.getClass().getSimpleName(), criterion);
                rtnVal.add(nextAcceptor);
            }
            else {
                log.trace("  [ ] {} is incompatible with criterion {}", nextAcceptor.getClass().getSimpleName(), criterion);
            }
        }
        return rtnVal;
    }

    public static DomainObjectHandler findFirstHelper(Class<? extends DomainObject> domainClass) {
        Collection<DomainObjectHandler> handlers = findHandler(domainClass);
        if (handlers.isEmpty()) return null;
        return handlers.iterator().next();
    }

    public static DomainObjectHandler findFirstHelper(DomainObject domainObject) {
        return findFirstHelper(domainObject.getClass());
    }
}
