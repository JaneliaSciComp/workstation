package org.janelia.it.workstation.gui.browser.api;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.DomainUtils;


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
        return DomainUtils.isOwner(domainObject, AccessManager.getSubjectKey());
    } 
    
    /**
     * Returns true if the current user has read access to the given domain object.
     * @param domainObject
     * @return
     */
    public static boolean hasReadAccess(DomainObject domainObject) {
        return DomainUtils.hasReadAccess(domainObject, AccessManager.getSubjectKey());
    }
    
    /**
     * Returns true if the current user has write access to the given domain object.
     * @param domainObject
     * @return
     */
    public static boolean hasWriteAccess(DomainObject domainObject) {
        return DomainUtils.hasWriteAccess(domainObject, AccessManager.getSubjectKey());
    }

}
