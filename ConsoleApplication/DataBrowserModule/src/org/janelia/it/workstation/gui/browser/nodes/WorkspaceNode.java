package org.janelia.it.workstation.gui.browser.nodes;


import com.google.common.collect.Lists;
import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.nb_action.AddToFolderAction;
import org.janelia.it.workstation.gui.browser.nb_action.RemoveAction;
import org.janelia.it.workstation.gui.browser.nodes.children.TreeNodeChildFactory;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.actions.RenameAction;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.datatransfer.PasteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkspaceNode extends TreeNodeNode {
    
    private final static Logger log = LoggerFactory.getLogger(WorkspaceNode.class);
    
    public WorkspaceNode(TreeNodeChildFactory parentChildFactory, TreeNode treeNode) {
        super(parentChildFactory, treeNode);
    }
    
    private WorkspaceNode(TreeNodeChildFactory parentChildFactory, final TreeNodeChildFactory childFactory, TreeNode treeNode) {
        super(parentChildFactory, childFactory, treeNode);
    }
    
    public TreeNode getTreeNode() {
        return (TreeNode)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getTreeNode().getName();
    }
    
    @Override
    public String getExtraLabel() {
        return "("+getTreeNode().getNumChildren()+")";
    }
    
    @Override
    public Image getIcon(int type) {
        if (!getTreeNode().getOwnerKey().equals(SessionMgr.getSubjectKey())) {
            return Icons.getIcon("folder_blue.png").getImage();
        }
        else {
            return Icons.getIcon("folder.png").getImage();    
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
    
    @Override
    public Action[] getActions(boolean context) {
        Action[] superActions = super.getActions(context);
        List<Action> actions = new ArrayList<>();
        actions.add(RenameAction.get(RenameAction.class));
        actions.addAll(Lists.newArrayList(superActions));
        return actions.toArray(new Action[0]);
    }
    
    @Override
    public PasteType getDropType(final Transferable t, int action, int index) {
        final TreeNode treeNode = getTreeNode();                
        if (t.isDataFlavorSupported(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR)) {
            try {
                DomainObject domainObject = (DomainObject) t.getTransferData(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR);
                log.info("Will paste {} on {}", domainObject.getId(), treeNode.getName());
            }
            catch (Exception ex) {
                log.error("Error pasting domain object", ex);
            }
            return new PasteType() {
                @Override
                public Transferable paste() throws IOException {
                    try {
                        DomainObject domainObject = (DomainObject) t.getTransferData(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR);
                        log.info("Pasting {} on {}",domainObject.getId(),treeNode.getName());
                        if (DomainUtils.hasChild(treeNode, domainObject)) {
                            log.info("Child already exists. TODO: should reorder it to the end");
                        }
                        else {
                            childFactory.addChild(domainObject);    
                        }
                        final Node node = NodeTransfer.node(t, NodeTransfer.DND_MOVE + NodeTransfer.CLIPBOARD_CUT);
                        if (node != null) {
                            log.info("Original node was moved or cut, so we presume it was pasted, and will destroy node");
                            node.destroy();
                        }
                    } catch (UnsupportedFlavorException ex) {
                        log.error("Flavor is not supported for paste",ex);
                    }
                    return null;
                }
            };
        } else {
            log.trace("Transfer does not support domain object flavor.");
            return null;
        }
    }
    
    @Override
    protected void createPasteTypes(Transferable t, List<PasteType> s) {
        super.createPasteTypes(t, s);
        PasteType p = getDropType(t, 0, 0);
        if (p != null) {
            s.add(p);
        }
    }
}
