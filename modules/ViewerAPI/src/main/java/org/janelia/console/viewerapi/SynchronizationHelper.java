
package org.janelia.console.viewerapi;

import java.util.ArrayList;
import org.openide.util.lookup.Lookups;
import java.util.Collection;
import org.openide.util.Lookup;

/**
 * Aids for synchronizing two high-level views.
 * @author fosterl
 */
public class SynchronizationHelper {
    /**
     * Obtain all possible sources of sample/location synchronization, except
     * for the one listed.  The excluded one is meant to reflect the name of
     * the location acceptor, who will not wish to synchronize with itself.
     * 
     * @param excludedName do not return a provider by this name.  May be null.
     * @return all providers who wish to advertise their locations/samples.
     */
    public Collection<Tiled3dSampleLocationProviderAcceptor> getSampleLocationProviders(String excludedName) {
        Lookup lookup = Lookups.forPath(Tiled3dSampleLocationProviderAcceptor.LOOKUP_PATH);
        Collection<? extends Tiled3dSampleLocationProviderAcceptor> candidates = 
                        lookup.lookupAll(Tiled3dSampleLocationProviderAcceptor.class);
        Collection<Tiled3dSampleLocationProviderAcceptor> rtnVal = new ArrayList<>();
        for ( Tiled3dSampleLocationProviderAcceptor provider: candidates ) {
            if ( !provider.getProviderUniqueName().equals( excludedName ) ) {
                rtnVal.add( provider );
            }
        }
        return rtnVal;
    }
    
    /**
     * Obtain provider by name listed. Converse of 
     * @See Collection<Tiled3dSampleLocationProviderAcceptor> #getSampleLocationProviders(String excludedName)
     * 
     * @param includedName ONLY return a provider by this name.  May NOT be null.
     * @return all providers who wish to advertise their locations/samples.
     */
    public Tiled3dSampleLocationProviderAcceptor getSampleLocationProviderByName(String includedName) {
        if (includedName == null) {
            throw new IllegalArgumentException("Must provide non-null provider name");
        }
        Tiled3dSampleLocationProviderAcceptor rtnVal = null;
        Lookup lookup = Lookups.forPath(Tiled3dSampleLocationProviderAcceptor.LOOKUP_PATH);
        Collection<? extends Tiled3dSampleLocationProviderAcceptor> candidates = 
                        lookup.lookupAll(Tiled3dSampleLocationProviderAcceptor.class);
        for ( Tiled3dSampleLocationProviderAcceptor provider: candidates ) {
            if ( provider.getProviderUniqueName().equals( includedName ) ) {
                rtnVal = provider;
            }
        }
        return rtnVal;
    }
}
