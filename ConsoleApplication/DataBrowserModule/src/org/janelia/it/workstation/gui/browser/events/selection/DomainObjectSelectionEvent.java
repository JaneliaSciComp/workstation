package org.janelia.it.workstation.gui.browser.events.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private final List<DomainObject> domainObjects;
    private final boolean select;
    private final boolean clearAll;
    private final boolean isUserDriven;

    public DomainObjectSelectionEvent(Object source, DomainObjectNode domainObjectNode, boolean select, boolean clearAll, boolean isUserDriven) {
        this.source = source;
        this.domainObjectNode = domainObjectNode;
        this.domainObjects = Arrays.asList(domainObjectNode.getDomainObject());
        this.select = select;
        this.clearAll = clearAll;
        this.isUserDriven = isUserDriven;
    }
    
    public DomainObjectSelectionEvent(Object source, List<? extends DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        this.source = source;
        this.domainObjectNode = null;
        this.domainObjects = new ArrayList<>(domainObjects);
        this.select = select;
        this.clearAll = clearAll;
        this.isUserDriven = isUserDriven;
    }

    public Object getSource() {
        return source;
    }
    
    public DomainObjectNode getDomainObjectNode() {
        return domainObjectNode;
    }

    public DomainObject getObjectIfSingle() {
        return domainObjects.size()==1 ? domainObjects.get(0) : null;
    }
    
    public List<DomainObject> getDomainObjects() {
        return domainObjects;
    }

    public boolean isSelect() {
        return select;
    }

    public boolean isClearAll() {
        return clearAll;
    }

    public boolean isUserDriven() {
        return isUserDriven;
    }

    @Override
    public String toString() {
        return "DomainObjectSelectionEvent [source=" + source + ", domainObjectNode=" + domainObjectNode + ", domainObjects=" + domainObjects
                + ", select=" + select + ", clearAll=" + clearAll + ", isUserDriven=" + isUserDriven + "]";
    }
}
