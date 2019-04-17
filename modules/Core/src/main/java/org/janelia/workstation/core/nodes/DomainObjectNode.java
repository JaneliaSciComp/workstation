package org.janelia.workstation.core.nodes;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasIdentifier;

/**
 * Interface for NetBeans Nodes which wrap domain objects.
 *
 * It's expected that any class which extends this interface also extends org.openide.nodes.Node. Unfortunately, that's
 * not an interface, so it can't be extended here.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectNode<T extends DomainObject> extends HasIdentifier {
    
    T getDomainObject();

    void update(T domainObject);
    
}
