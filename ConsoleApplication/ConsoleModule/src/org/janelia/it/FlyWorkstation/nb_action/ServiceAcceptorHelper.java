package org.janelia.it.FlyWorkstation.nb_action;

import java.util.Collection;
import org.openide.util.lookup.Lookups;

/**
 * Delegate class to facilitate dealing with finding compatible acceptors
 * for various tasks.  Convenience class that cuts down on redundant code.
 * 
 * @author fosterl
 */
public class ServiceAcceptorHelper {
    /**
     * Do a generic lookup, for things that can deal with found values on
     * the outline.
     * 
     * @param T type of the thing to return.
     * @param S type of things to check.
     * @param criterion this is used to judge compatibility of found items.
     * @param clazz tells what type of thing to find.
     * @param path tells the path identifier for searching compatible items.
     * @return a compatible handler for the criterion object.
     */
    public <T extends Compatible,S> T findHandler(S criterion, Class clazz, String path) {
        Collection<T> candidates
                = Lookups.forPath(path).lookupAll(clazz);

        T rtnVal = null;
        for (T nextAcceptor : candidates) {
            if (nextAcceptor.isCompatible(criterion)) {
                rtnVal = nextAcceptor;
                break;
            }
        }
        return rtnVal;
    }
}
