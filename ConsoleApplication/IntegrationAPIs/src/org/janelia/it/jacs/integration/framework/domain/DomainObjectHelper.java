package org.janelia.it.jacs.integration.framework.domain;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

/**
 * A service that generates Nodes for domain objects.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectHelper extends Compatible<DomainObject> {
    
    public static final String DOMAIN_OBJECT_LOOKUP_PATH = "DomainObject/DomainObjectNodeProvider";
    
    /**
     * Can this service generate a node for the given domain object?
     */
    @Override
    boolean isCompatible(DomainObject domainObject);
    
    /**
     * Actually create a node for the given domain object.
     * @param e
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
