package org.janelia.workstation.core.events.selection;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A selection model which tracks user domain object selections for annotation.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GlobalDomainObjectSelectionModel extends SelectionModel<DomainObject,Reference> {

    private static final Logger log = LoggerFactory.getLogger(GlobalDomainObjectSelectionModel.class);
    
    public static GlobalDomainObjectSelectionModel instance;
    
    private GlobalDomainObjectSelectionModel() {
    }
    
    public static GlobalDomainObjectSelectionModel getInstance() {
        if (instance==null) {
            instance = new GlobalDomainObjectSelectionModel();
        }
        return instance;
    }

    @Override
    protected void selectionChanged(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        log.info("selectionChanged({}, select={}, clearAll={}, isUserDriven={})", DomainUtils.abbr(domainObjects), select, clearAll, isUserDriven);
        log.info("Global selection: {}", DomainUtils.abbr(getSelectedIds()));
    }
    
    @Override
    public Reference getId(DomainObject domainObject) {
        return Reference.createFor(domainObject);
    }
}
