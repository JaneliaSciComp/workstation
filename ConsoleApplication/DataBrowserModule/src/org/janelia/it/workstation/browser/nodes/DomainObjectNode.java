package org.janelia.it.workstation.browser.nodes;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasIdentifier;

/**
 * Interface for NetBeans Nodes which wrap domain objects.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectNode<T extends DomainObject> extends HasIdentifier {
    
    public T getDomainObject();

    public void update(T domainObject);
    
}
