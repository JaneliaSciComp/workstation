package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.nodes.ObjectSetNode;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectSetSelectionEvent extends DomainObjectSelectionEvent {

    public ObjectSetSelectionEvent(Object source, Reference identifier, boolean select, ObjectSet objectSet) {
        super(source, identifier, objectSet, select, true);
    }
    
    public ObjectSetSelectionEvent(Object source, Reference identifier, boolean select, ObjectSetNode objectSetNode) {
        super(source, identifier, objectSetNode, select, true);
    }
    
    public ObjectSetNode getObjectSetNode() {
        return (ObjectSetNode)getDomainObjectNode();
    }
    
    public ObjectSet getObjectSet() {
        return (ObjectSet)getDomainObject();
    }
}
