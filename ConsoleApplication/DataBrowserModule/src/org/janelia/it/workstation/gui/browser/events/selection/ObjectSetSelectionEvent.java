package org.janelia.it.workstation.gui.browser.events.selection;

import java.util.Arrays;

import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.nodes.ObjectSetNode;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectSetSelectionEvent extends DomainObjectSelectionEvent {

    public ObjectSetSelectionEvent(Object source, boolean select, ObjectSet objectSet, boolean isUserDriven) {
        super(source, Arrays.asList(objectSet), select, true, isUserDriven);
    }
    
    public ObjectSetSelectionEvent(Object source, boolean select, ObjectSetNode objectSetNode, boolean isUserDriven) {
        super(source, objectSetNode, select, true, isUserDriven);
    }
    
    public ObjectSetNode getObjectSetNode() {
        return (ObjectSetNode)getDomainObjectNode();
    }
    
    public ObjectSet getObjectSet() {
        return (ObjectSet)getObjectIfSingle();
    }
}
