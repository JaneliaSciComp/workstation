package org.janelia.it.workstation.gui.browser.actions;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class RemoveAction extends NodeAction {

    private final static Logger log = LoggerFactory.getLogger(RemoveAction.class);
    
    private static RemoveAction singleton = new RemoveAction();
    
    private List<Node> selected = new ArrayList<Node>();
    
    public static RemoveAction get() {
        return singleton;
    }

    
    public RemoveAction() {
        putValue(NAME, "Remove");
    }
    
//
//    @Override
//    public boolean isEnabled() {
//        return true;
//        
//        DomainObject domainObject = getLookup().lookup(DomainObject.class);
//        return (domainObject instanceof Folder) && DomainUtils.hasWriteAccess(domainObject);
//    }
    
    @Override
    protected void performAction (Node[] activatedNodes) {
        enable(activatedNodes);
        for(Node node : selected) {
            log.info("remove "+node.getDisplayName()+" from "+node.getParentNode().getDisplayName());
        }
        
    }
    
    @Override
    protected boolean asynchronous() {
        return true;
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        selected.clear();
        for(Node node : activatedNodes) {
            
            boolean included = true;
        
            Node parentNode = node.getParentNode();
            
            if (parentNode instanceof TreeNodeNode) {
                TreeNodeNode parentTreeNodeNode = (TreeNodeNode)parentNode;
                TreeNode parentTreeNode = parentTreeNodeNode.getTreeNode();
                // Must have write access to parent
                if (!DomainUtils.hasWriteAccess(parentTreeNode)) {
                    included = false;
                }
            }
            else {
                included = false;
            }
            
            if (node instanceof DomainObjectNode) {
                DomainObjectNode domainObjectNode = (DomainObjectNode)node;
                DomainObject domainObject = domainObjectNode.getDomainObject();
            }
            else {
                included = false;
            }
            
            if (included) {
                selected.add(node);
            }
        }
        return selected.size()==activatedNodes.length;
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(RemoveAction.class, "Remove");
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(RemoveAction.class);
    }
}
