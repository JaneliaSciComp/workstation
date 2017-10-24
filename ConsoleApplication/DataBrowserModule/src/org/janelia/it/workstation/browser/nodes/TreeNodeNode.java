package org.janelia.it.workstation.browser.nodes;

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.Action;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.CopyToClipboardAction;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.browser.flavors.DomainObjectNodeFlavor;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.nb_action.AddToFolderAction;
import org.janelia.it.workstation.browser.nb_action.DownloadAction;
import org.janelia.it.workstation.browser.nb_action.NewDomainObjectAction;
import org.janelia.it.workstation.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.browser.nb_action.RemoveAction;
import org.janelia.it.workstation.browser.nb_action.RenameAction;
import org.janelia.it.workstation.browser.nb_action.SearchHereAction;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.workspace.TreeNode;
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
public class TreeNodeNode extends AbstractDomainObjectNode<TreeNode> {
    
    private final static Logger log = LoggerFactory.getLogger(TreeNodeNode.class);

    // We use a single threaded executor so that all node operations are serialized. Re-ordering operations 
    // in particular must be done sequentially, for obvious reasons.
    private static final Executor nodeOperationExecutor = Executors.newSingleThreadExecutor();
    
    private TreeNodeChildFactory childFactory;
    
    public TreeNodeNode(ChildFactory<?> parentChildFactory, TreeNode treeNode) {
        this(parentChildFactory, new TreeNodeChildFactory(treeNode), treeNode);
    }
    
    private TreeNodeNode(ChildFactory<?> parentChildFactory, final TreeNodeChildFactory childFactory, TreeNode treeNode) {
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
                    
                    // Get current set of child nodes before going async
                    final Node[] nodes = getNodes();
                    
                    SimpleWorker worker = new SimpleWorker() {
                        @Override
                        protected void doStuff() throws Exception {

                            // This is a little tricky for two reasons: 
                            // a) The order parameter is not the reordered indexes of the nodes, as you'd expect
                            //    but rather the new positions of each of the nodes in the old sequence. The tricky
                            //    part is that these can be identical in many cases, so it's important to test all
                            //    the corner cases.
                            // b) The order only gives the new order for the nodes in the tree, but we have to account 
                            //    for children of the TreeNode which are not represented as nodes. Currently, this  
                            //    code just throws those non-node children at the end every time there's a reordering, 
                            //    but eventually we'll want to make it preserve the ordering. 

                            log.info("Reordering nodes with new permutation: {}", Arrays.toString(order));
        
                            // Get current children for the node
                            final List<Reference> children = getTreeNode().getChildren();
                                                        
                            // Build new list of ordered nodes
                            Map<Reference, Integer> newPositions = new HashMap<>();
                            for(int i=0; i<nodes.length; i++) {
                                Node node = nodes[i];
                                if (node instanceof DomainObjectNode) {
                                    int newPos = order[i];
                                    DomainObjectNode<?> domainObjectNode = (DomainObjectNode<?>)node;
                                    DomainObject domainObject = domainObjectNode.getDomainObject();
                                    newPositions.put(Reference.createFor(domainObject), newPos);
                                    log.debug("Setting node {} at {}", domainObject.getName(), newPos);
                                }
                                else {
                                    throw new IllegalStateException("Encountered node that is not DomainObjectNode");
                                }
                            }
                            
                            // Add the objects which are not represented as nodes
                            int nextIndex = newPositions.size();
                            for(Reference ref : children) {
                                if (!newPositions.containsKey(ref)) {
                                    int newPos = nextIndex++;
                                    newPositions.put(ref, newPos);
                                    log.debug("Setting non-node {} at {}", ref, newPos);
                                }
                            }
                            
                            // Build the real reordering array, including non-nodes
                            int[] trueOrder = new int[children.size()];
                            int i = 0;
                            for(Reference ref : children) {
                                int newPos = newPositions.get(ref);
                                trueOrder[i++] = newPos; 
                            }
        
                            log.info("Reordering children with new permutation: {}", Arrays.toString(trueOrder));
                            DomainMgr.getDomainMgr().getModel().reorderChildren(getTreeNode(), trueOrder);
                        }
                        @Override
                        protected void hadSuccess() {
                            // GUI will be updated by events 
                        }
                        @Override
                        protected void hadError(Throwable error) {
                            ConsoleApp.handleException(error);
                        }
                    };
                    
                    nodeOperationExecutor.execute(worker);
                }
            });
        }
    }
    
    /**
     * This method should be called whenever the underlying domain object changes. It updates the UI to reflect the new object state.
     */
    @Override
    public void update(TreeNode treeNode) {
        log.debug("Refreshing node@{} -> {}",System.identityHashCode(this),getDisplayName());
        super.update(treeNode);
        log.debug("Refreshing children for {} (now has {} children)", treeNode.getName(), treeNode.getNumChildren());
        childFactory.update(treeNode);
        refreshChildren();
    }
    
    /**
     * Updates the UI state of the nodes to reflect the object state. 
     */
    private void refreshChildren() {
        // Update the child factory
        childFactory.refresh();
        // Ensure that the node has the correct type of "Children" object (leaf or non-leaf)
        boolean isLeaf = getChildren()==Children.LEAF;
        boolean hasChildren = childFactory.hasNodeChildren();
        if (isLeaf == hasChildren) {
            log.debug("Node {} changed child-having status",getDisplayName());
            this.setChildren(createChildren());
        }
    }

    /**
     * Creates the correct type of "Children" object (leaf or non-leaf)
     */
    private Children createChildren() {
        if (childFactory.hasNodeChildren()) {
            childFactory = new TreeNodeChildFactory(getTreeNode());
            return Children.create(childFactory, false);
        }
        else {
            return Children.LEAF;
        }
    }
    
    public TreeNode getTreeNode() {
        return getDomainObject();
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
        actions.add(new CopyToClipboardAction("Name", getName()));
        actions.add(new CopyToClipboardAction("GUID", getId()+""));
        actions.add(null);
        actions.add(new OpenInViewerAction());
        actions.add(new OpenInNewViewerAction());
        actions.add(null);
        actions.add(new ViewDetailsAction());
        actions.add(new ChangePermissionsAction());
        actions.add(NewDomainObjectAction.get());
        actions.add(AddToFolderAction.get());
        actions.add(RenameAction.get());
        actions.add(RemoveAction.get());
        actions.add(null);
        actions.add(SearchHereAction.get());
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
    @SuppressWarnings("unchecked")
    public PasteType getDropType(final Transferable t, int action, final int index) {

        if (!ClientDomainUtils.hasWriteAccess(getTreeNode())) {
            return null;
        }

        List<AbstractDomainObjectNode<?>> nodes = new ArrayList<>();
        
        if (t.isDataFlavorSupported(DomainObjectNodeFlavor.SINGLE_FLAVOR)) {
            AbstractDomainObjectNode<?> node = DomainObjectNodeFlavor.getDomainObjectNode(t);
            if (node==null || !(node instanceof AbstractDomainObjectNode)) { 
                return null;
            }
            log.trace("  Single drop - {} with parent {}",node.getDisplayName(),node.getParentNode().getDisplayName());
            nodes.add(node);
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
                    AbstractDomainObjectNode<?> node = DomainObjectNodeFlavor.getDomainObjectNode(st);
                    if (node==null || !(node instanceof AbstractDomainObjectNode)) {
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
        
        private final List<AbstractDomainObjectNode<?>> nodes;
        private final TreeNodeNode targetNode;
        private final int startingIndex;
        
        TreeNodePasteType(List<AbstractDomainObjectNode<?>> nodes, TreeNodeNode targetNode, int startingIndex) {
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
                for(AbstractDomainObjectNode<?> node : nodes) {
                    if (node.getParentNode() instanceof TreeNodeNode) {
                        TreeNodeNode originalParentNode = (TreeNodeNode)node.getParentNode();
                        TreeNode originalParent = originalParentNode.getTreeNode();
                        originalParents.add(originalParent);
                    }
                    else {
                        originalParents.add(null);
                    }
                }
                
                List<DomainObject> toAdd = new ArrayList<>();
                List<AbstractDomainObjectNode<?>> toDestroy = new ArrayList<>();
                
                int i = 0;
                for(AbstractDomainObjectNode<?> node : nodes) {

                    DomainObject domainObject = node.getDomainObject();
                    
                    TreeNode originalParent = originalParents.get(i);
                    if (originalParent!=null) {
                        log.trace("{} has parent {}",newParent.getId(),originalParent.getId());
                    }

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

                    if (originalParent!=null && ClientDomainUtils.hasWriteAccess(originalParent)) {
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
                for(AbstractDomainObjectNode<?> node : toDestroy) {
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
