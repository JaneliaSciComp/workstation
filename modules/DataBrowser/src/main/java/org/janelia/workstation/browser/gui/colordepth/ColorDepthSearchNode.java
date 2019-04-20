package org.janelia.workstation.browser.gui.colordepth;

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.janelia.model.domain.workspace.Node;
import org.janelia.workstation.browser.flavors.DomainObjectNodeFlavor;
import org.janelia.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.browser.nodes.TreeNodeNode;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.NodeTransfer;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.MultiTransferObject;
import org.openide.util.datatransfer.PasteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColorDepthSearchNode extends AbstractDomainObjectNode<ColorDepthSearch> {

    private static final Logger log = LoggerFactory.getLogger(ColorDepthMaskNode.class);
    
    public ColorDepthSearchNode(ChildFactory<?> parentChildFactory, ColorDepthSearch mask) throws Exception {
        super(parentChildFactory, Children.LEAF, mask);
    }
    
    public ColorDepthSearch getColorDepthSearch() {
        return getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getColorDepthSearch().getName();
    }
        
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getColorDepthSearch())) {
            return Icons.getIcon("drive_magnify.png").getImage();
        }
        else {
            return Icons.getIcon("drive_magnify.png").getImage();
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
    public PasteType getDropType(final Transferable t, int action, final int index) {

        if (!ClientDomainUtils.hasWriteAccess(getDomainObject())) {
            return null;
        }

        List<ColorDepthMaskNode> nodes = new ArrayList<>();
        
        if (t.isDataFlavorSupported(DomainObjectNodeFlavor.SINGLE_FLAVOR)) {
            AbstractDomainObjectNode<?> node = DomainObjectNodeFlavor.getDomainObjectNode(t);
            if (node==null || !(node instanceof ColorDepthMaskNode)) { 
                return null;
            }
            log.trace("  Single drop - {} with parent {}",node.getDisplayName(),node.getParentNode().getDisplayName());
            nodes.add((ColorDepthMaskNode)node);
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
                    if (node==null || !(node instanceof ColorDepthMaskNode)) {
                        continue;
                    }   
                    log.trace("  Multi drop #{} - {} with parent {}",i,node.getDisplayName(),node.getParentNode().getDisplayName());
                    nodes.add((ColorDepthMaskNode)node);
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
        
        // Only allow those masks which match the search's alignment space
        nodes = nodes.stream().filter(mask -> mask.getColorDepthMask().getAlignmentSpace().equals(getColorDepthSearch().getAlignmentSpace())).collect(Collectors.toList());
        
        if (!nodes.isEmpty()) {
            return new SearchPasteType(nodes, this);
        }

        return null;
    }
    
    private class SearchPasteType extends PasteType {
        
        private final List<ColorDepthMaskNode> nodes;
        private final ColorDepthSearchNode targetNode;
        
        SearchPasteType(List<ColorDepthMaskNode> nodes, ColorDepthSearchNode targetNode) {
            log.trace("TreeNodePasteType with {} nodes and target {}",nodes.size(),targetNode.getName());
            this.nodes = nodes;
            this.targetNode = targetNode;
        }
        
        @Override
        public String getName() {
            return "PasteIntoTreeNode";
        }
        @Override
        public Transferable paste() throws IOException {
            try {
                log.trace("paste called on SearchPasteType with {} nodes and target {}",nodes.size(),targetNode.getName());
                ColorDepthSearch newParent = targetNode.getColorDepthSearch();
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
                
                ColorDepthSearch search = targetNode.getColorDepthSearch();
                for(ColorDepthMaskNode node : nodes) {
                    ColorDepthMask mask = node.getColorDepthMask();
                    DomainMgr.getDomainMgr().getModel().addMaskToSearch(search, mask);
                }
                
            } 
            catch (Exception e) {
                throw new IOException("Error pasting node",e);
            }
            return null;
        }
    }
}
