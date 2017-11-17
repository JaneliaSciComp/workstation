package org.janelia.it.workstation.browser.api;

import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;

/**
 * Miscellaneous utility methods for dealing with the Domain model on the client side. Generic utility methods for the domain model 
 * are found in the DomainUtils class in the model module. This class only deals with things specific to the client side.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ClientDomainUtils {

    /**
     * Returns true if the current user owns the given domain object.
     * @param domainObject
     * @return
     */
    public static boolean isOwner(DomainObject domainObject) {
        if (!AccessManager.loggedIn()) return false;
        return DomainUtils.isOwner(domainObject, AccessManager.getSubjectKey());
    } 
    
    /**
     * Returns true if the current user has read access to the given domain object.
     * @param domainObject can they read this?
     * @return T=Yes; F=No
     */
    public static boolean hasReadAccess(DomainObject domainObject) {
        if (!AccessManager.loggedIn()) return false;
        return DomainUtils.hasReadAccess(domainObject, AccessManager.getReaderSet());
    }
    
    /**
     * Returns true if the current user has write access to the given domain object.
     * @param domainObject can they write this?
     * @return T=Yes; F=No
     */
    public static boolean hasWriteAccess(DomainObject domainObject) {
        if (!AccessManager.loggedIn()) return false;
        return DomainUtils.hasWriteAccess(domainObject, AccessManager.getWriterSet());
    }
}
