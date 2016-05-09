package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A selection model implementation which tracks the selection of domain objects, including
 * a single parent object which contains multiple child objects. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectSelectionModel extends SelectionModel<DomainObject,Reference> {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectSelectionModel.class);
    
    private IsParent parentObject;
    
    public IsParent getParentObject() {
        return parentObject;
    }

    public void setParentObject(IsParent parentObject) {
        this.parentObject = parentObject;
    }

    @Override
    protected void selectionChanged(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        log.debug("selectionChanged(objects.size={}, select={}, clearAll={}, isUserDriven={})",domainObjects.size(),select,clearAll,isUserDriven);
        if (domainObjects.size()==1) {
            DomainObject domainObject = domainObjects.get(0);
            if (domainObject instanceof TreeNode) {
                TreeNode treeNode = (TreeNode)domainObject;
                Events.getInstance().postOnEventBus(new TreeNodeSelectionEvent(getSource(), select, treeNode, isUserDriven));
                return;
            }
        }
        
        Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(getSource(), domainObjects, select, clearAll, isUserDriven));
    }
    
    @Override
    public Reference getId(DomainObject domainObject) {
        return Reference.createFor(domainObject);
    }
}
