package org.janelia.it.workstation.nb_action;

import java.util.Collection;
import java.util.ArrayList;
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
     * @param T type of the thing to return in collection.
     * @param S type of things to check.
     * @param criterion this is used to judge compatibility of found items.
     * @param clazz tells what type of thing to find.
     * @param path tells the path identifier for searching compatible items.
     * @return a compatible handler for the criterion object.
     */
    public<T extends Compatible,S> Collection<T> findHandler(S criterion, Class clazz, String path) {
        Collection<T> candidates
                = Lookups.forPath(path).lookupAll(clazz);

        Collection<T> rtnVal = new ArrayList<T>(); 
        for (T nextAcceptor : candidates) {
            if (nextAcceptor.isCompatible(criterion)) {
                rtnVal.add(nextAcceptor);
            }
        }
        return rtnVal;
    }
}
