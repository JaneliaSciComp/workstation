package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectNodeFlavor;
import org.janelia.it.workstation.gui.browser.nb_action.DownloadAction;
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
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.MultiTransferObject;
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
        super(parentChildFactory, childFactory.hasNodeChildren()?Children.create(childFactory, false):Children.LEAF, treeNode);
            
        log.trace("Creating node@{} -> {}",System.identityHashCode(this),getDisplayName());

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
                            // GUI will be updated by events 
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

    public final void checkChildren() {
        boolean isLeaf = getChildren()==Children.LEAF;
        boolean hasChildren = childFactory.hasNodeChildren();
        if (isLeaf == hasChildren) {
            log.trace("Node {} changed child-having status",getDisplayName());
            this.setChildren(createChildren());
        }
    }

    public Children createChildren() {
        if (childFactory.hasNodeChildren()) {
            return Children.create(new TreeNodeChildFactory(getTreeNode()), false);
        }
        else {
            return Children.LEAF;
        }
    }
    
    @Override
    public void update(DomainObject domainObject) {
        super.update(domainObject);
        TreeNode treeNode = (TreeNode)domainObject;
        log.trace("Refreshing node@{} -> {}",System.identityHashCode(this),getDisplayName());
        log.debug("Refreshing children for {} (now has {} children)",domainObject.getName(),treeNode.getNumChildren());
        childFactory.update(treeNode);
        refreshChildren();
    }
    
    public void refreshChildren() {
        childFactory.refresh();
        checkChildren();
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
        if (ClientDomainUtils.isOwner(getTreeNode())) {
            return Icons.getIcon("folder.png").getImage();
        }
        else {
            return Icons.getIcon("folder_blue.png").getImage();
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
        actions.add(null);
        actions.add(DownloadAction.get());
        return actions.toArray(new Action[0]);
    }

    @Override
    protected void createPasteTypes(Transferable t, List<PasteType> s) {
        super.createPasteTypes(t, s);
        PasteType dropType = getDropType(t, NodeTransfer.CLIPBOARD_COPY, -1);
        if (dropType!=null) s.add(dropType);
    }
    
    @Override
    public PasteType getDropType(final Transferable t, int action, final int index) {

        if (!ClientDomainUtils.hasWriteAccess(getTreeNode())) {
            return null;
        }

        List<DomainObjectNode> nodes = new ArrayList<>();
        
        if (t.isDataFlavorSupported(DomainObjectNodeFlavor.SINGLE_FLAVOR)) {
            DomainObjectNode node = DomainObjectNodeFlavor.getDomainObjectNode(t);
            if (node==null || !(node instanceof DomainObjectNode)) { 
                return null;
            }
            log.trace("  Single drop - {} with parent {}",node.getDisplayName(),node.getParentNode().getDisplayName());
            nodes = Arrays.asList(node);
        }
        else if (t.isDataFlavorSupported(DomainObjectFlavor.LIST_FLAVOR)) {
            final List<DomainObject> objects;
            try {
                objects = (List<DomainObject>) t.getTransferData(DomainObjectFlavor.LIST_FLAVOR);
            }
            catch (UnsupportedFlavorException | IOException e) {
                log.error("Error getting drop type", e);
                return null;
            }
            return new PasteType() {
                @Override
                public String getName() {
                    return "PasteIntoObjectSet";
                }
                @Override
                public Transferable paste() throws IOException {
                    try {
                        DomainModel model = DomainMgr.getDomainMgr().getModel();
                        if (index<0) {
                            model.addChildren(getTreeNode(), objects);
                        }
                        else {
                            model.addChildren(getTreeNode(), objects, index);
                        }
                    }
                    catch (Exception e) {
                        throw new IOException("Error pasting into object set",e);
                    }
                    return null;
                }
            };
        }
        else if (t.isDataFlavorSupported(ExTransferable.multiFlavor)) {
            MultiTransferObject multi;
            try {
                multi = (MultiTransferObject) t.getTransferData(ExTransferable.multiFlavor);
            }
            catch (UnsupportedFlavorException | IOException e) {
                log.error("Error getting transfer data", e);
                return null;
            }

            for(int i=0; i<multi.getCount(); i++) {
                Transferable st = multi.getTransferableAt(i);
                if (st.isDataFlavorSupported(DomainObjectNodeFlavor.SINGLE_FLAVOR)) {
                    DomainObjectNode node = DomainObjectNodeFlavor.getDomainObjectNode(st);
                    if (node==null || !(node instanceof DomainObjectNode)) {
                        continue;
                    }   
                    log.trace("  Multi drop #{} - {} with parent {}",i,node.getDisplayName(),node.getParentNode().getDisplayName());
                    nodes.add(node);
                }
                else {
                    log.trace("Multi-transferable is expected to support DomainObjectNodeFlavor.");
                }
            }
        }
        else {
            log.trace("Transferable is expected to support either DomainObjectNodeFlavor or multiFlavor.");
            return null;
        }

        if (!nodes.isEmpty()) {
            return new TreeNodePasteType(nodes, this, index);
        }

        return null;
    }

    private class TreeNodePasteType extends PasteType {
        
        private final List<DomainObjectNode> nodes;
        private final TreeNodeNode targetNode;
        private final int startingIndex;
        
        TreeNodePasteType(List<DomainObjectNode> nodes, TreeNodeNode targetNode, int startingIndex) {
            log.trace("TreeNodePasteType with {} nodes and target {}",nodes.size(),targetNode.getName());
            this.nodes = nodes;
            this.targetNode = targetNode;
            this.startingIndex = startingIndex;
        }
        
        @Override
        public String getName() {
            return "PasteIntoTreeNode";
        }
        @Override
        public Transferable paste() throws IOException {
            try {
                log.trace("paste called on TreeNodePasteType with {} nodes and target {}",nodes.size(),targetNode.getName());
                TreeNode newParent = targetNode.getTreeNode();
                
                // Have to keep track of the original parents before we do anything, 
                // because once we start moving nodes, the parents will be recreated
                List<TreeNode> originalParents = new ArrayList<>();
                for(DomainObjectNode node : nodes) {
                    TreeNodeNode originalParentNode = (TreeNodeNode)node.getParentNode();
                    TreeNode originalParent = originalParentNode.getTreeNode();
                    originalParents.add(originalParent);
                }
                
                List<DomainObject> toAdd = new ArrayList<>();
                List<DomainObjectNode> toDestroy = new ArrayList<>();
                
                int i = 0;
                for(DomainObjectNode node : nodes) {

                    DomainObject domainObject = node.getDomainObject();
                    
                    TreeNode originalParent = originalParents.get(i);
                    log.trace("{} has parent {}",newParent.getId(),originalParent.getId());

                    if (domainObject.getId().equals(newParent.getId())) {
                        log.info("Cannot move a node into itself: {}",domainObject.getId());
                        continue;
                    }
                    else if (newParent.hasChild(domainObject)) {
                        log.info("Child already exists: {}",domainObject.getId());
                        continue;
                    }
                    log.info("Pasting '{}' on '{}'",domainObject.getName(),newParent.getName());
                    toAdd.add(domainObject);

                    if (ClientDomainUtils.hasWriteAccess(originalParent)) {
                        toDestroy.add(node);
                    }
                    
                    i++;
                }
                
                // Add all the nodes 
                if (!toAdd.isEmpty()) {
                    if (startingIndex<0) {
                        childFactory.addChildren(toAdd);    
                    }
                    else {
                        childFactory.addChildren(toAdd, startingIndex);
                    }
                }
                
                // Remove the originals
                for(DomainObjectNode node : toDestroy) {
                    node.destroy();
                }
                
            } 
            catch (Exception e) {
                throw new IOException("Error pasting node",e);
            }
            return null;
        }
    }

}
