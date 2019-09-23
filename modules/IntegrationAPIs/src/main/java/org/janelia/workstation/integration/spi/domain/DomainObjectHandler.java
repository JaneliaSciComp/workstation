package org.janelia.workstation.integration.spi.domain;

import org.janelia.model.domain.DomainObject;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

/**
 * A helper service for making your domain objects interoperable with other core modules.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectHandler extends Compatible<DomainObject> {

    /**
     * Can this service handle the given object?
     */
    @Override
    boolean isCompatible(DomainObject domainObject);

    /**
     * Can this service handle the given object class?
     * @param clazz
     * @return
     */
    boolean isCompatible(Class<? extends DomainObject> clazz);
    
    /**
     * Create a node for the given domain object.
     * @param domainObject
     * @param parentChildFactory child factory which is requesting this node
     * @return
     */
    <T extends DomainObject> Node getNode(T domainObject, ChildFactory<T> parentChildFactory) throws Exception;
    
    /**
     * Returns the class of the editor to use when a node with the given type is selected in the data explorer. 
     * @param domainObject
     * @return
     */
    public Class<?> getEditorClass(DomainObject domainObject);
    
    /**
     * Returns a large static icon for the given domain object.
     * @param domainObject
     * @return
     */
    String getLargeIcon(DomainObject domainObject);

    /**
     * Returns true if removal of the given object is supported by this helper.
     * @return
     */
    boolean supportsRemoval(DomainObject domainObject);

    /**
     * Returns the maximum number of references that can exist to the object before its safe to remove.
     * @return
     */
    default int getMaxReferencesBeforeRemoval(DomainObject domainObject) {
        return 1;
    }

    /**
     * Domain-specific deletion.
     * @param domainObject
     * @throws Exception
     */
    void remove(DomainObject domainObject) throws Exception;
    
    
}
