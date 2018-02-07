package org.janelia.it.workstation.browser.nb_action;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.workstation.browser.actions.RemoveItemsFromFolderAction;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.it.workstation.browser.nodes.TreeNodeNode;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.domain.workspace.TreeNode;
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

    private final List<org.openide.nodes.Node> selected = new ArrayList<>();
    private List<DomainObject> toRemove = new ArrayList<>();
    private Node parentTreeNode;
    
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
    protected boolean enable(org.openide.nodes.Node[] activatedNodes) {
        selected.clear();
        toRemove.clear();
        parentTreeNode = null;
        for(org.openide.nodes.Node node : activatedNodes) {
            selected.add(node);
            
            boolean included = true;
        
            org.openide.nodes.Node parentNode = node.getParentNode();
            
            if (parentNode instanceof TreeNodeNode) {
                TreeNodeNode parentTreeNodeNode = (TreeNodeNode)parentNode;
                Node parentTreeNode = parentTreeNodeNode.getNode();
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
                included = true;
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
    protected void performAction (org.openide.nodes.Node[] activatedNodes) {
        RemoveItemsFromFolderAction action = new RemoveItemsFromFolderAction(parentTreeNode, toRemove);
        action.actionPerformed(null);
    }
}
