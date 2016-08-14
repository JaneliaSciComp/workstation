package org.janelia.it.workstation.gui.browser.nb_action;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action which implements node removal for Folders and Workspaces. 
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
    private final List<Node> toRemove = new ArrayList<>();
    
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
        for(Node node : activatedNodes) {
            
            boolean included = true;
        
            Node parentNode = node.getParentNode();
            
            if (parentNode instanceof TreeNodeNode) {
                TreeNodeNode parentTreeNodeNode = (TreeNodeNode)parentNode;
                TreeNode parentTreeNode = parentTreeNodeNode.getTreeNode();
                // Must have write access to parent
                if (!ClientDomainUtils.hasWriteAccess(parentTreeNode)) {
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
            
            selected.add(node);
            if (included) {
                toRemove.add(node);
            }
        }
        return toRemove.size()==selected.size();
    }
    
    @Override
    protected void performAction (Node[] activatedNodes) {

        ActivityLogHelper.logUserAction("RemoveAction.performAction");

        final DomainModel model = DomainMgr.getDomainMgr().getModel();
        final Multimap<TreeNode,DomainObject> removeFromFolders = ArrayListMultimap.create();
        final List<Reference> listToDelete = new ArrayList<>();

        for(Node node : toRemove) {
            if (node instanceof DomainObjectNode) {
                TreeNodeNode parentNode = (TreeNodeNode)node.getParentNode();
                if (parentNode==null) {
                    log.warn("Node has no parent, so it cannot be deleted: "+node.getDisplayName());
                }
                else {
                    TreeNode treeNode = parentNode.getTreeNode();
                    DomainObject domainObject = ((DomainObjectNode)node).getDomainObject();

                    // first check to make sure this Object only has one ancestor references; if it does pop up a dialog before removal
                    List<Reference> refList = model.getContainerReferences (domainObject);
                    if (refList==null || refList.size()<=1) {
                        listToDelete.add(Reference.createFor(domainObject));
                    }
                    removeFromFolders.put(treeNode,domainObject);
                }
            }
            else {
                throw new IllegalStateException("Remove can only be called on DomainObjectNodes");
            }
        }

        if (!listToDelete.isEmpty()) {
            int deleteConfirmation = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(),
                    "There are " + listToDelete.size() + " items in your remove list that will be deleted permanently.",
                    "Are you sure?", JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation != 0) {
                return;
            }
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                // Remove references
                for (TreeNode treeNode : removeFromFolders.keySet()) {
                    for (DomainObject domainObject: removeFromFolders.get(treeNode)) {
                        model.removeChild(treeNode, domainObject);
                    }
                }

                // Remove any actual objects that are no longer references
                if (!listToDelete.isEmpty()) {
                    model.remove(listToDelete);
                }
            }

            @Override
            protected void hadSuccess() {
                // Handled by the event system
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }
}
