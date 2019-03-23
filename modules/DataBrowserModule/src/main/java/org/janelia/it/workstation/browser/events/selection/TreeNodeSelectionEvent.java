package org.janelia.it.workstation.browser.events.selection;

import org.janelia.it.workstation.browser.nodes.TreeNodeNode;
import org.janelia.model.domain.workspace.TreeNode;

import java.util.Arrays;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TreeNodeSelectionEvent extends DomainObjectSelectionEvent {

    public TreeNodeSelectionEvent(Object source, TreeNode treeNode, boolean select, boolean clearAll, boolean isUserDriven) {
        super(source, Arrays.asList(treeNode), select, clearAll, isUserDriven);
    }
    
    public TreeNodeSelectionEvent(Object source, TreeNodeNode treeNodeNode, boolean select, boolean clearAll, boolean isUserDriven) {
        super(source, treeNodeNode, select, clearAll, isUserDriven);
    }
    
    public TreeNodeNode getTreeNodeNode() {
        return (TreeNodeNode)getDomainObjectNode();
    }
    
    public TreeNode getObjectSet() {
        return (TreeNode)getObjectIfSingle();
    }
}
