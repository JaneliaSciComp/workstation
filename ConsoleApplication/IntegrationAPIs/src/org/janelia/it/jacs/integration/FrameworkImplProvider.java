/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.integration;

import java.util.Collection;
import org.janelia.it.jacs.integration.framework.session_mgr.SessionSupport;
import org.openide.util.lookup.Lookups;

/**
 * The factory to return implementations from the framework.
 *
 * @author fosterl
 */
public class FrameworkImplProvider {
    public static SessionSupport getSessonSupport() {
        Collection<? extends SessionSupport> candidates
                = Lookups.forPath(SessionSupport.LOOKUP_PATH).lookupAll(SessionSupport.class);
        if (candidates.size() > 0) {
            return candidates.iterator().next();
        }
        else {
            return null;
        }
    }
}
