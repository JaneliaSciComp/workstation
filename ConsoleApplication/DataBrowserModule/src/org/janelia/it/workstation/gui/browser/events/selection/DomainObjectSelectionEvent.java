package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectSelectionEvent {

    private final DomainObjectId identifier;
    private final DomainObjectNode domainObjectNode;
    private final DomainObject domainObject;
    private final SelectionType selectionType;
    private final boolean clearAll;

    public DomainObjectSelectionEvent(DomainObjectId identifier, DomainObjectNode domainObjectNode, SelectionType selectionType, boolean clearAll) {
        this.identifier = identifier;
        this.domainObjectNode = domainObjectNode;
        this.domainObject = domainObjectNode.getDomainObject();
        this.selectionType = selectionType;
        this.clearAll = clearAll;
    }
    
    public DomainObjectSelectionEvent(DomainObjectId identifier, DomainObject domainObject, SelectionType selectionType, boolean clearAll) {
        this.identifier = identifier;
        this.domainObjectNode = null;
        this.domainObject = domainObject;
        this.selectionType = selectionType;
        this.clearAll = clearAll;
    }

    public DomainObjectId getIdentifier() {
        return identifier;
    }
    
    public DomainObjectNode getDomainObjectNode() {
        return domainObjectNode;
    }
    
    public DomainObject getDomainObject() {
        return domainObject;
    }

    public SelectionType getSelectionType() {
        return selectionType;
    }

    public boolean isClearAll() {
        return clearAll;
    }
}
