package org.janelia.workstation.core.api.facade.interfaces;

import java.util.Collection;
import java.util.List;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ReverseReference;
import org.janelia.model.domain.report.DatabaseSummary;
import org.janelia.model.domain.report.DiskUsageSummary;

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
    <T extends DomainObject> T getDomainObject(Class<T> domainClass, Long id) throws Exception;

    /**
     * Returns the domain object with the given class and name.
     * @param domainClass class of the domain object
     * @param name name of the domain object
     * @return
     */
    <T extends DomainObject> List<T> getDomainObjects(Class<T> domainClass, String name) throws Exception;
    
    /**
     * Returns the domain object specified by the given reference.
     * @param reference to a domain object
     * @return the domain object
     */
    <T extends DomainObject> T getDomainObject(Reference reference) throws Exception;

    /**
     * Returns the domain objects specified by the given list of references. 
     * @param references list of references
     * @return list of domain objects
     */
    List<DomainObject> getDomainObjects(List<Reference> references) throws Exception;

    /**
     * Returns the domain objects specified by the given reverse reference. 
     * @param reference reverse reference
     * @return list of domain objects
     */
    List<DomainObject> getDomainObjects(ReverseReference reference) throws Exception;

    /**
     * Returns domain objects with the given property value.
     * @param className
     * @param propertyName
     * @param propertyValue
     * @param <T>
     * @return
     * @throws Exception
     */
    <T extends DomainObject> List<T> getDomainObjectsWithProperty(String className, String propertyName, String propertyValue) throws Exception;

    /**
     * Returns the domain objects of a particular type, given by the list of GUIDs. 
     * @param className class name
     * @param ids collection of GUIDs
     * @return list of domain objects
     */
    <T extends DomainObject> List<T> getDomainObjects(String className, Collection<Long> ids) throws Exception;

    /**
     * Returns the domain objects of a particular type, for ANY ownership.
     * @param className class name
     * @return list of domain objects
     */
    List<DomainObject> getAllDomainObjectsByClass(String className) throws Exception;
    
    /**
     * Update a property on the given domain object.
     * @param domainObject domain object to update
     * @param propName name of property to update
     * @param propValue new property value
     * @return the updated domain object
     */
    DomainObject updateProperty(DomainObject domainObject, String propName, Object propValue) throws Exception;

    /**
     * Update the permissions on the given domain object to grant or revoke rights to some subject. 
     * @param domainObject the domain object for which to change permissions 
     * @param granteeKey the subject key being granted or revoked permission
     * @param rights list of access rights, e.g. "rw"
     * @throws Exception something went wrong
     */
    DomainObject setPermissions(DomainObject domainObject, String granteeKey, String rights) throws Exception;

    /**
     * 
     * @param deleteObjectRefs
     * @throws Exception
     */
    void remove(List<Reference> deleteObjectRefs) throws Exception;

    /**
     * Remove object storage.
     */
    void removeObjectStorage(List<String> storagePaths);

    /**
     *
     * For individual cases, adds support to create a new DomainObject or update an existing one
     * @param  domainObject domain object to update
     * @throws Exception
     */
    DomainObject save(DomainObject domainObject) throws Exception;
    
    /**
     * Returns the database summary for the current user.
     * @return
     * @throws Exception
     */
    DatabaseSummary getDatabaseSummary() throws Exception;
    
    /**
     * Returns the disk usage summary for the current user.
     * @return
     * @throws Exception
     */
    DiskUsageSummary getDiskUsageSummary() throws Exception;
}
