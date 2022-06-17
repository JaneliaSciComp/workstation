package org.janelia.workstation.integration.spi.domain;

import com.google.common.collect.ComparisonChain;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.workspace.Workspace;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
        List<? extends DomainObjectHandler> candidates =
                new ArrayList<>(Lookup.getDefault().lookupAll(DomainObjectHandler.class));
        Collection<DomainObjectHandler> rtnVal = new ArrayList<>();
        log.trace("Found DomainObjectHandler handlers:");
        // Sort in reverse order so that lowest position candidates are screened first
        candidates.sort((o1, o2) -> ComparisonChain.start().compare(getPosition(o2), getPosition(o1)).result());
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

    private static <T extends DomainObjectHandler> int getPosition(T handler) {
        ServiceProvider[] annotations = handler.getClass().getAnnotationsByType(ServiceProvider.class);
        if (annotations.length == 1) {
            ServiceProvider annotation = annotations[0];
            return annotation.position();
        }
        return Integer.MAX_VALUE;
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
