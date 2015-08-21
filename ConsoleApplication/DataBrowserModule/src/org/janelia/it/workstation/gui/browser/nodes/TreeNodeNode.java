package org.janelia.it.workstation.gui.browser.nodes;


import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.nb_action.MoveToFolderAction;
import org.janelia.it.workstation.gui.browser.nb_action.NewDomainObjectAction;
import org.janelia.it.workstation.gui.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.gui.browser.nb_action.RemoveAction;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Index;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.datatransfer.PasteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tree node (i.e. Folder) in the data graph. Supports reordering, 
 * drag and drop, and context menu actions. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TreeNodeNode extends DomainObjectNode {
    
    private final static Logger log = LoggerFactory.getLogger(TreeNodeNode.class);
    
    private final TreeNodeChildFactory childFactory;
    
    public TreeNodeNode(ChildFactory parentChildFactory, TreeNode treeNode) {
        this(parentChildFactory, new TreeNodeChildFactory(treeNode), treeNode);
    }
    
    private TreeNodeNode(ChildFactory parentChildFactory, final TreeNodeChildFactory childFactory, TreeNode treeNode) {
        super(parentChildFactory, Children.create(childFactory, false), treeNode);
        log.debug("Creating new tree node for {}",treeNode.getName());
        this.childFactory = childFactory;
        if (treeNode.getNumChildren()>0) {
            getLookupContents().add(new Index.Support() {
                @Override
                public Node[] getNodes() {
                    return getChildren().getNodes();
                }
                @Override
                public int getNodesCount() {
                    return getNodes().length;
                }
                @Override
                public void reorder(final int[] order) {
                    SimpleWorker worker = new SimpleWorker() {
                        @Override
                        protected void doStuff() throws Exception {
                            DomainModel model = DomainMgr.getDomainMgr().getModel();
                            model.reorderChildren(getTreeNode(), order);
                        }
                        @Override
                        protected void hadSuccess() {
                            refreshChildren();
                        }
                        @Override
                        protected void hadError(Throwable error) {
                            SessionMgr.getSessionMgr().handleException(error);
                        }
                    };
                    worker.execute();
                }
            });
        }
    }

    @Override
    public void update(DomainObject domainObject) {
        super.update(domainObject);
        TreeNode treeNode = (TreeNode)domainObject;
        log.debug("Refreshing children for {} (now has {} children)",domainObject.getName(),treeNode.getNumChildren());
        childFactory.update(treeNode);
        childFactory.refresh();
    }
    
    public void refreshChildren() {
        childFactory.refresh();
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
        List<Action> actions = new ArrayList<>();
        actions.add(PopupLabelAction.get());
        actions.add(null);
        actions.add(new CopyNameAction());
        actions.add(new CopyGUIDAction());
        actions.add(null);
        actions.add(NewDomainObjectAction.get());
        actions.add(MoveToFolderAction.get());
        actions.add(new RenameAction());
        actions.add(RemoveAction.get());
        return actions.toArray(new Action[0]);
    }

    @Override
    public PasteType getDropType(final Transferable t, int action, int index) {
        
        if (t.isDataFlavorSupported(DomainObjectFlavor.SINGLE_FLAVOR)) {

            final Node node = NodeTransfer.node(t, NodeTransfer.DND_MOVE + NodeTransfer.CLIPBOARD_CUT);
            if (node==null) {
                // Only accept nodes. This filters out drag and drop from other components, like the Data Browser. 
                return null;
            }   

            final TreeNode treeNode = getTreeNode();
            final TreeNode originalParent = ((TreeNodeNode)node.getParentNode()).getTreeNode();
            log.trace("{} has parent {}",treeNode.getId(),originalParent.getId());
            
            DomainObject domainObject;
            try {
                domainObject = (DomainObject) t.getTransferData(DomainObjectFlavor.SINGLE_FLAVOR);
            }
            catch (UnsupportedFlavorException | IOException e) {
                log.error("Error getting drop type", e);
                return null;
            }

            log.trace("Will paste {} on {}",domainObject.getId(),treeNode.getName());
            
            final DomainObject toPaste = domainObject;
            return new PasteType() {
                @Override
                public String getName() {
                    return "PasteIntoTreeNode";
                }
                @Override
                public Transferable paste() throws IOException {
                    try {
                        if (toPaste.getId().equals(treeNode.getId())) {
                            log.info("Cannot move a node into itself");
                            return null;
                        }
                        else if (DomainUtils.hasChild(treeNode, toPaste)) {
                            log.info("Child already exists.");
                            return null;
                        }
                        log.info("Pasting {} on {}",toPaste.getId(),treeNode.getName());
                        childFactory.addChild(toPaste);
                        if (DomainUtils.hasWriteAccess(originalParent)) {
                            log.info("Original node was moved or cut, so we presume it was pasted, and will destroy node");
                            node.destroy();
                        }
                    } 
                    catch (Exception e) {
                        throw new IOException("Error pasting node",e);
                    }
                    return null;
                }
            };
        }
        else {
            log.debug("Transfer data does not support domain object list flavor.");
            return null;
        }
        
    }
}
