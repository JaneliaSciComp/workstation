package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;

import java.util.Arrays;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TreeNodeSelectionEvent extends DomainObjectSelectionEvent {

    public TreeNodeSelectionEvent(Object source, boolean select, TreeNode treeNode, boolean isUserDriven) {
        super(source, Arrays.asList(treeNode), select, true, isUserDriven);
    }
    
    public TreeNodeSelectionEvent(Object source, boolean select, TreeNodeNode treeNodeNode, boolean isUserDriven) {
        super(source, treeNodeNode, select, true, isUserDriven);
    }
    
    public TreeNodeNode getTreeNodeNode() {
        return (TreeNodeNode)getDomainObjectNode();
    }
    
    public TreeNode getObjectSet() {
        return (TreeNode)getObjectIfSingle();
    }
}
