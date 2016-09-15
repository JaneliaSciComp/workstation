package org.janelia.it.jacs.integration.framework.domain;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

/**
 * A helper service for making your domain objects interoperable with other core modules.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectHelper extends Compatible<DomainObject> {
    
    public static final String DOMAIN_OBJECT_LOOKUP_PATH = "DomainObject/DomainObjectNodeProvider";
    
    /**
     * Can this service handle the given object?
     */
    @Override
    boolean isCompatible(DomainObject domainObject);
    
    /**
     * Create a node for the given domain object.
     * @param domainObject
     * @param parentChildFactory child factory which is requesting this node
     * @return
     */
    Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception;
 
    /**
     * Returns a large static icon for the given domain object.
     * @param domainObject
     * @return
     */
    String getLargeIcon(DomainObject domainObject);

    /**
     * Domain-specific deletion.
     * @param domainObject
     * @throws Exception
     */
    void remove(DomainObject domainObject) throws Exception;
    
    
}
