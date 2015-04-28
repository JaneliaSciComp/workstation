package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.nodes.ObjectSetNode;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectSetSelectionEvent extends DomainObjectSelectionEvent {

    public ObjectSetSelectionEvent(DomainObjectId identifier, ObjectSet objectSet) {
        super(identifier, objectSet, SelectionType.Explorer, true);
    }
    
    public ObjectSetSelectionEvent(DomainObjectId identifier, ObjectSetNode objectSetNode) {
        super(identifier, objectSetNode, SelectionType.Explorer, true);
    }
    
    public ObjectSetNode getObjectSetNode() {
        return (ObjectSetNode)getDomainObjectNode();
    }
    
    public ObjectSet getObjectSet() {
        return (ObjectSet)getDomainObject();
    }
}
