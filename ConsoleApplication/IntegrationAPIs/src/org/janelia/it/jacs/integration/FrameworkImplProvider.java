/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.integration;

import java.util.Collection;
import org.janelia.it.jacs.integration.framework.compression.CompressedFileResolverI;
import org.janelia.it.jacs.integration.framework.session_mgr.ActivityLogging;
import org.openide.util.lookup.Lookups;

/**
 * The factory to return implementations from the framework.
 *
 * @author fosterl
 */
public class FrameworkImplProvider {
    public static ActivityLogging getSessionSupport() {
        Collection<? extends ActivityLogging> candidates
                = Lookups.forPath(ActivityLogging.LOOKUP_PATH).lookupAll(ActivityLogging.class);
        if (candidates.size() > 0) {
            return candidates.iterator().next();
        }
        else {
            return null;
        }
    }
    
    public static CompressedFileResolverI getCompressedFileResolver() {
        Collection<? extends CompressedFileResolverI> candidates
                = Lookups.forPath(CompressedFileResolverI.LOOKUP_PATH).lookupAll(CompressedFileResolverI.class);
        if (candidates.size() > 0) {
            return candidates.iterator().next();            
        }
        else {
            return null;
        }
    }
}
