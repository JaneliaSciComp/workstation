package org.janelia.it.workstation.browser.nb_action;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.browser.actions.RemoveItemsFromFolderAction;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.it.workstation.browser.nodes.TreeNodeNode;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

/**
 * Action which implements node removal for Folders and Workspaces. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class RemoveAction extends NodeAction {

    private final static RemoveAction singleton = new RemoveAction();
    public static RemoveAction get() {
        return singleton;
    }

    private final List<Node> selected = new ArrayList<>();
    private List<DomainObject> toRemove = new ArrayList<>();
    private TreeNode parentTreeNode;
    
    private RemoveAction() {
    }
    
    @Override
    public String getName() {
        return "Remove "+toRemove.size()+" items";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("RemoveAction");
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        selected.clear();
        toRemove.clear();
        parentTreeNode = null;
        for(Node node : activatedNodes) {
            selected.add(node);
            
            boolean included = true;
        
            Node parentNode = node.getParentNode();
            
            if (parentNode instanceof TreeNodeNode) {
                TreeNodeNode parentTreeNodeNode = (TreeNodeNode)parentNode;
                TreeNode parentTreeNode = parentTreeNodeNode.getTreeNode();
                if (this.parentTreeNode==null) {
                    this.parentTreeNode = parentTreeNode;
                }
                else if (!this.parentTreeNode.getId().equals(parentTreeNode.getId())) {
                    // Wrong parent
                    included = false;
                }
                // Must have write access to parent
                if (!ClientDomainUtils.hasWriteAccess(parentTreeNode)) {
                    included = false;
                }
            }
            else {
                included = false;
            }
            
            if (node instanceof AbstractDomainObjectNode) {
                AbstractDomainObjectNode<?> domainObjectNode = (AbstractDomainObjectNode<?>)node;
                DomainObject domainObject = domainObjectNode.getDomainObject();
                if (included) {
                    toRemove.add(domainObject);
                }
            }
        }
        return toRemove.size()==selected.size();
    }
    
    @Override
    protected void performAction (Node[] activatedNodes) {
        RemoveItemsFromFolderAction action = new RemoveItemsFromFolderAction(parentTreeNode, toRemove);
        action.actionPerformed(null);
    }
}
