package org.janelia.it.workstation.browser.events.selection;

import java.util.ArrayList;
import java.util.List;

import org.janelia.model.domain.DomainObject;

/**
 * Event indicating that a domain object's edit selection has changed.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectEditSelectionEvent {

    private final Object source;
    private final List<DomainObject> domainObjects;
    private final boolean select;
    private final boolean clearAll;
    private final boolean isUserDriven;
    
    public DomainObjectEditSelectionEvent(Object source, List<? extends DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        this.source = source;
        this.domainObjects = new ArrayList<>(domainObjects);
        this.select = select;
        this.clearAll = clearAll;
        this.isUserDriven = isUserDriven;
    }

    public Object getSource() {
        return source;
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
        String s = source == null ? null : source.getClass().getSimpleName();
        return "DomainObjectEditSelectionEvent [source=" + s + ", domainObjects=" + domainObjects
                + ", select=" + select + ", clearAll=" + clearAll + ", isUserDriven=" + isUserDriven + "]";
    }
}
