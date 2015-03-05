/* 
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
     * @return all providers who wish to advertize their locations/samples.
     */
    public Collection<Tiled3dSampleLocationProvider> getSampleLocationProviders(String excludedName) {
        Lookup lookup = Lookups.forPath(Tiled3dSampleLocationProvider.LOOKUP_PATH);
        Collection<? extends Tiled3dSampleLocationProvider> candidates = 
                        lookup.lookupAll(Tiled3dSampleLocationProvider.class);
        Collection<Tiled3dSampleLocationProvider> rtnVal = new ArrayList<>();
        for ( Tiled3dSampleLocationProvider provider: candidates ) {
            if ( !provider.getProviderUniqueName().equals( excludedName ) ) {
                rtnVal.add( provider );
            }
        }
        return rtnVal;
    }
}
