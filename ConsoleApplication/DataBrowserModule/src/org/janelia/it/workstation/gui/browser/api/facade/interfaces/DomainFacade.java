package org.janelia.it.workstation.gui.browser.api.facade.interfaces;

import java.util.Collection;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;

/**
 * Implementations provide generic access to domain objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainFacade {

    /**
     * Returns the domain object of a given class with the given GUID. 
     * @param domainClass class of domain object
     * @param id GUID of domain object
     * @return the domain object
     */
    public <T extends DomainObject> T getDomainObject(Class<T> domainClass, Long id);

    /**
     * Returns the domain object with the given class and name.
     * @param domainClass class of the domain object
     * @param name name of the domain object
     * @return
     */
    public <T extends DomainObject> List<T> getDomainObjects(Class<T> domainClass, String name);
    
    /**
     * Returns the domain object specified by the given reference.
     * @param reference to a domain object
     * @return the domain object
     */
    public DomainObject getDomainObject(Reference reference);

    /**
     * Returns the domain objects specified by the given list of references. 
     * @param references list of references
     * @return list of domain objects
     */
    public List<DomainObject> getDomainObjects(List<Reference> references);

    /**
     * Returns the domain objects specified by the given reverse reference. 
     * @param reference reverse reference
     * @return list of domain objects
     */
    public List<DomainObject> getDomainObjects(ReverseReference reference);

    /**
     * Returns the domain objects of a particular type, given by the list of GUIDs. 
     * @param className class name
     * @param ids collection of GUIDs
     * @return list of domain objects
     */
    public List<DomainObject> getDomainObjects(String className, Collection<Long> ids);

    /**
     * Returns the domain objects of a particular type, for ANY ownership.
     * @param className class name
     * @return list of domain objects
     */
    public List<DomainObject> getAllDomainObjectsByClass(String className);
    
    /**
     * Update a property on the given domain object.
     * @param domainObject domain object to update
     * @param propName name of property to update
     * @param propValue new property value
     * @return the updated domain object
     */
    public DomainObject updateProperty(DomainObject domainObject, String propName, Object propValue) throws Exception;

    /**
     * Update the permissions on the given domain object to grant or revoke rights to some subject. 
     * @param domainObject the domain object for which to change permissions 
     * @param granteeKey the subject key being granted or revoked permission
     * @param rights list of access rights, e.g. "rw"
     * @param grant grant or revoke?
     * @throws Exception something went wrong
     */
    public DomainObject changePermissions(DomainObject domainObject, String granteeKey, String rights, boolean grant) throws Exception;

    /**
     * 
     * @param deleteObjectRefs
     * @throws Exception
     */
    public void remove(List<Reference> deleteObjectRefs) throws Exception;

}
