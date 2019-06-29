package org.janelia.workstation.core.events.selection;

import java.util.List;

import org.janelia.model.domain.DomainObject;

/**
 * Event indicating that a domain object's selection has changed.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectSelectionEvent extends ObjectSelectionEvent<DomainObject> {

    public DomainObjectSelectionEvent(Object source, List<? extends DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        super(source, domainObjects, select, clearAll, isUserDriven);
    }

    public Object getSource() {
        return getSourceComponent();
    }
    
    public List<DomainObject> getDomainObjects() {
        return getObjects();
    }
}
