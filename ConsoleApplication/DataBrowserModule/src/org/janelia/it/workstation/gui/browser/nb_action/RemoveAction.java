package org.janelia.it.workstation.gui.browser.nb_action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class RemoveAction extends NodeAction {

    private final static Logger log = LoggerFactory.getLogger(RemoveAction.class);
    
    private final static RemoveAction singleton = new RemoveAction();
    public static RemoveAction get() {
        return singleton;
    }
    
    private final List<Node> selected = new ArrayList<>();
    
    private RemoveAction() {
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
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
    protected void performAction (Node[] activatedNodes) {
        Set<TreeNodeNode> toRefresh = new HashSet<>();
        
        for(Node node : selected) {
            try {
                // TODO: check number of references
                TreeNodeNode treeNodeNode = (TreeNodeNode)node.getParentNode();
                TreeNode treeNode = (treeNodeNode).getTreeNode();
                DomainObject domainObject = ((DomainObjectNode)node).getDomainObject();
                DomainDAO dao = DomainMgr.getDomainMgr().getDao();
                dao.removeChild(SessionMgr.getSubjectKey(), treeNode, domainObject);
                // TODO: delete the object if there are no more references
                toRefresh.add(treeNodeNode);
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
        }
        
        for(TreeNodeNode treeNodeNode : toRefresh) {
            treeNodeNode.refresh();
        }
        
    }
    
    @Override
    public String getName() {
        return "Remove "+selected.size()+" items";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("RemoveAction");
    }
}
