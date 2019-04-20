package org.janelia.workstation.browser.nodes;

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.workspace.Node;
import org.janelia.workstation.browser.flavors.DomainObjectFlavor;
import org.janelia.workstation.browser.flavors.DomainObjectNodeFlavor;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Index;
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
public class TreeNodeNode extends AbstractDomainObjectNode<Node> {
    
    private final static Logger log = LoggerFactory.getLogger(TreeNodeNode.class);
    
    private TreeNodeChildFactory childFactory;
    
    public TreeNodeNode(ChildFactory<?> parentChildFactory, Node treeNode) {
        this(parentChildFactory, new TreeNodeChildFactory(treeNode), treeNode);
    }
    
    private TreeNodeNode(ChildFactory<?> parentChildFactory, final TreeNodeChildFactory childFactory, Node treeNode) {
        super(parentChildFactory, childFactory.hasNodeChildren()?Children.create(childFactory, false):Children.LEAF, treeNode);
            
        log.trace("Creating node@{} -> {}",System.identityHashCode(this),getDisplayName());

        this.childFactory = childFactory;
        if (treeNode.getNumChildren()>0) {
            getLookupContents().add(new Index.Support() {
                
                @Override
                public org.openide.nodes.Node[] getNodes() {
                    return getChildren().getNodes();
                }
                
                @Override
                public int getNodesCount() {
                    return getNodes().length;
                }
                
                @Override
                public void reorder(final int[] order) {
                    
                    // Get current set of child nodes before going async
                    final org.openide.nodes.Node[] nodes = getNodes();
                    
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
                            final List<Reference> children = getNode().getChildren();
                                                        
                            // Build new list of ordered nodes
                            Map<Reference, Integer> newPositions = new HashMap<>();
                            for(int i=0; i<nodes.length; i++) {
                                org.openide.nodes.Node node = nodes[i];
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
                            DomainMgr.getDomainMgr().getModel().reorderChildren(getNode(), trueOrder);
                        }
                        @Override
                        protected void hadSuccess() {
                            // GUI will be updated by events 
                        }
                        @Override
                        protected void hadError(Throwable error) {
                            FrameworkAccess.handleException(error);
                        }
                    };
                    
                    NodeUtils.executeNodeOperation(worker);
                }
            });
        }
    }
    
    /**
     * This method should be called whenever the underlying domain object changes. It updates the UI to reflect the new object state.
     */
    @Override
    public void update(Node treeNode) {
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
            childFactory = new TreeNodeChildFactory(getNode());
            return Children.create(childFactory, false);
        }
        else {
            return Children.LEAF;
        }
    }
    
    public Node getNode() {
        return getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getNode().getName();
    }

    @Override
    public String getExtraLabel() {
        return "("+getNode().getNumChildren()+")";
    }

    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getNode())) {
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
    protected void createPasteTypes(Transferable t, List<PasteType> s) {
        super.createPasteTypes(t, s);
        PasteType dropType = getDropType(t, NodeTransfer.CLIPBOARD_COPY, -1);
        if (dropType!=null) s.add(dropType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public PasteType getDropType(final Transferable t, int action, final int index) {

        if (!ClientDomainUtils.hasWriteAccess(getNode())) {
            return null;
        }

        List<AbstractDomainObjectNode<?>> nodes = new ArrayList<>();
        
        if (t.isDataFlavorSupported(DomainObjectNodeFlavor.SINGLE_FLAVOR)) {
            AbstractDomainObjectNode<?> node = DomainObjectNodeFlavor.getDomainObjectNode(t);
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
                            model.addChildren(getNode(), objects);
                        }
                        else {
                            model.addChildren(getNode(), objects, index);
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
            return new NodePasteType(nodes, this, index);
        }

        return null;
    }

    private class NodePasteType extends PasteType {
        
        private final List<AbstractDomainObjectNode<?>> nodes;
        private final TreeNodeNode targetNode;
        private final int startingIndex;
        
        NodePasteType(List<AbstractDomainObjectNode<?>> nodes, TreeNodeNode targetNode, int startingIndex) {
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
                Node newParent = targetNode.getNode();
                if (newParent==null) {
                    log.warn("Target node has no TreeNode: "+targetNode.getDisplayName());
                    return null;
                }
                
                // Have to keep track of the original parents before we do anything, 
                // because once we start moving nodes, the parents will be recreated
                List<Node> originalParents = new ArrayList<>();
                for(AbstractDomainObjectNode<?> node : nodes) {
                    if (node.getParentNode() instanceof TreeNodeNode) {
                        TreeNodeNode originalParentNode = (TreeNodeNode)node.getParentNode();
                        Node originalParent = originalParentNode.getNode();
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
                    
                    Node originalParent = originalParents.get(i);
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
