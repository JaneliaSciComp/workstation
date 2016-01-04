package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;

/**
 * Event indicating that a domain object's selection has changed.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectSelectionEvent {

    private final Object source;
    private final DomainObjectNode domainObjectNode;
    private final DomainObject domainObject;
    private final boolean select;
    private final boolean clearAll;

    public DomainObjectSelectionEvent(Object source, DomainObjectNode domainObjectNode, boolean select, boolean clearAll) {
        this.source = source;
        this.domainObjectNode = domainObjectNode;
        this.domainObject = domainObjectNode.getDomainObject();
        this.select = select;
        this.clearAll = clearAll;
    }
    
    public DomainObjectSelectionEvent(Object source, DomainObject domainObject, boolean select, boolean clearAll) {
        this.source = source;
        this.domainObjectNode = null;
        this.domainObject = domainObject;
        this.select = select;
        this.clearAll = clearAll;
    }

    public Object getSource() {
        return source;
    }
    
    public DomainObjectNode getDomainObjectNode() {
        return domainObjectNode;
    }
    
    public DomainObject getDomainObject() {
        return domainObject;
    }

    public boolean isSelect() {
        return select;
    }

    public boolean isClearAll() {
        return clearAll;
    }

    @Override
    public String toString() {
        return "DomainObjectSelectionEvent[" + "source=" + source + ", domainObjectNode=" + domainObjectNode + ", domainObject=" + domainObject + ", select=" + select + ", clearAll=" + clearAll + ']';
    }
}
