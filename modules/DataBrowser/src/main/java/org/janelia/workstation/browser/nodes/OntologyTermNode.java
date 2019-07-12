package org.janelia.workstation.browser.nodes;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;

import org.janelia.model.domain.interfaces.HasIdentifier;
import org.janelia.model.domain.ontology.*;
import org.janelia.workstation.browser.actions.context.ApplyAnnotationAction;
import org.janelia.workstation.browser.actions.OntologyElementAction;
import org.janelia.workstation.common.flavors.OntologyTermFlavor;
import org.janelia.workstation.common.flavors.OntologyTermNodeFlavor;
import org.janelia.workstation.common.nodes.InternalNode;
import org.janelia.workstation.common.nodes.NodeUtils;
import org.janelia.workstation.core.actions.DomainObjectAcceptorHelper;
import org.janelia.workstation.browser.gui.components.OntologyExplorerTopComponent;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.keybind.KeyBindings;
import org.janelia.workstation.core.keybind.KeyboardShortcut;
import org.janelia.workstation.core.keybind.KeymapUtil;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
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
 * A node in an ontology. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyTermNode extends InternalNode<OntologyTerm> implements HasIdentifier {
    
    private final static Logger log = LoggerFactory.getLogger(OntologyTermNode.class);

    private final OntologyChildFactory childFactory;
    
    public OntologyTermNode(OntologyChildFactory parentChildFactory, Ontology ontology, OntologyTerm ontologyTerm) {
        this(parentChildFactory, new OntologyChildFactory(ontology, ontologyTerm), ontology, ontologyTerm);    
    }
    
    private OntologyTermNode(OntologyChildFactory parentChildFactory, OntologyChildFactory childFactory, final Ontology ontology, OntologyTerm ontologyTerm) {
        super(parentChildFactory, childFactory.hasNodeChildren()?Children.create(childFactory, false):Children.LEAF, ontologyTerm);
        
        log.trace("Creating node@{} -> {}",System.identityHashCode(this),getDisplayName());

        this.childFactory = childFactory;
        getLookupContents().add(ontology);
        if (ontologyTerm.getNumChildren()>0) {
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
                            OntologyTerm parentTerm = getOntologyTerm();
                            model.reorderOntologyTerms(ontology.getId(), parentTerm.getId(), order);
                        }
                        @Override
                        protected void hadSuccess() {
                            // Event model will refresh UI
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

    protected OntologyChildFactory getChildFactory() {
        return childFactory;
    }
    
    public OntologyTermNode getParent() {
        Node parent = getParentNode();
        return parent instanceof OntologyTermNode ? (OntologyTermNode)parent : null;
    }

    public OntologyTerm getObject() {
        return getLookup().lookup(OntologyTerm.class);
    }
    
    public OntologyNode getOntologyNode() {
        Node node = this;
        while (node != null) {
            if (node instanceof OntologyNode) {
                return (OntologyNode)node;
            }
            node = node.getParentNode();
        }
        return null;
    }
    
    public Ontology getOntology() {
        return getLookup().lookup(Ontology.class);
    }
    
    public OntologyTerm getOntologyTerm() {
        return getObject();
    }

    @Override
    public String getName() {
        return getObject().getName();
    }
    
    @Override
    public Long getId() {
        return getOntologyTerm().getId();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getOntologyTerm().getName();
    }
    
    @Override
    public String getSecondaryLabel() {
        return getOntologyTerm().getTypeName();
    }
    
    @Override
    public String getExtraLabel() {
        OntologyExplorerTopComponent explorer = OntologyExplorerTopComponent.getInstance();
        OntologyElementAction action = explorer.getActionForTerm(getOntologyTerm());
        if (action != null) {
            KeyboardShortcut bind = KeyBindings.getKeyBindings().getBinding(action);
            if (bind != null) {
                return "(" + KeymapUtil.getShortcutText(bind) + ")";
            }
        }
        return null;
    }
    
    public void fireShortcutChanged() {
        fireDisplayNameChange(null, getDisplayName());
    }
    
    @Override
    public Image getIcon(int type) {
        OntologyTerm term = getOntologyTerm();
        if (term instanceof Category) {
            return Icons.getIcon("folder.png").getImage();
        }
        else if (term instanceof org.janelia.model.domain.ontology.Enum) {
            return Icons.getIcon("folder_page.png").getImage();
        }
        else if (term instanceof Interval) {
            return Icons.getIcon("page_white_code.png").getImage();
        }
        else if (term instanceof Tag) {
            return Icons.getIcon("page_white.png").getImage();
        }
        else if (term instanceof Accumulation) {
            return Icons.getIcon("page_white_edit.png").getImage();
        }
        else if (term instanceof Custom) {
            return Icons.getIcon("page_white_text.png").getImage();
        }
        else if (term instanceof Text) {
            return Icons.getIcon("page_white_text.png").getImage();
        }
        else if (term instanceof EnumItem) {
            return Icons.getIcon("page.png").getImage();
        }
        else if (term instanceof EnumText) {
            return Icons.getIcon("page_go.png").getImage();
        }
        return Icons.getIcon("bullet_error.png").getImage();
    }

    @Override
    public boolean canCut() {
        return ClientDomainUtils.hasWriteAccess(getOntology());
    }

    @Override
    public boolean canCopy() {
        return true;
    }

    @Override
    public boolean canRename() {
        return false;
    }

    @Override
    public boolean canDestroy() {
        return ClientDomainUtils.hasWriteAccess(getOntology());
    }
    
    @Override
    public Action[] getActions(boolean context) {
        Collection<Action> actions = DomainObjectAcceptorHelper.getCurrentContextActions();
        return actions.toArray(new Action[0]);
    }

    @Override
    public Action getPreferredAction() {
        return ApplyAnnotationAction.get();
    }

    @Override
    public Transferable clipboardCopy() throws IOException {
        log.debug("Copy to clipboard: {}",getOntologyTerm());
        Transferable deflt = super.clipboardCopy();
        return addFlavors(ExTransferable.create(deflt));
    }

    @Override
    public Transferable clipboardCut() throws IOException {
        log.debug("Cut to clipboard: {}",getOntologyTerm());
        Transferable deflt = super.clipboardCut();
        return addFlavors(ExTransferable.create(deflt));
    }
    
    private Transferable addFlavors(ExTransferable added) {
        added.put(new ExTransferable.Single(DataFlavor.stringFlavor) {
            @Override
            protected String getData() {
                return getPrimaryLabel();
            }
        });
        added.put(new ExTransferable.Single(OntologyTermFlavor.SINGLE_FLAVOR) {
            @Override
            protected OntologyTerm getData() {
                return getOntologyTerm();
            }
        });
        added.put(new ExTransferable.Single(OntologyTermNodeFlavor.SINGLE_FLAVOR) {
            @Override
            protected OntologyTermNode getData() {
                return OntologyTermNode.this;
            }
        });
        return added;
    }

    @Override
    protected void createPasteTypes(Transferable t, List<PasteType> s) {
        super.createPasteTypes(t, s);
        PasteType dropType = getDropType(t, NodeTransfer.CLIPBOARD_COPY, -1);
        if (dropType!=null) s.add(dropType);
    }
    
    @Override
    public PasteType getDropType(final Transferable t, int action, final int index) {

        if (!ClientDomainUtils.hasWriteAccess(getOntology())) {
            return null;
        }
        
        if (t.isDataFlavorSupported(OntologyTermNodeFlavor.SINGLE_FLAVOR)) {
            OntologyTermNode node = getOntologyTermNode(t);
            if (node==null || node.getParentNode() == null || !(node instanceof OntologyTermNode)) { 
                return null;
            }
            log.debug("  Single drop - {} with parent {}",node.getDisplayName(),node.getParentNode().getDisplayName());
            return new OntologyTermPasteType(Arrays.asList(node), this, index);
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
            
            List<OntologyTermNode> nodes = new ArrayList<>();
            for(int i=0; i<multi.getCount(); i++) {
                Transferable st = multi.getTransferableAt(i);
                if (st.isDataFlavorSupported(OntologyTermNodeFlavor.SINGLE_FLAVOR)) {
                    OntologyTermNode node = getOntologyTermNode(st);
                    if (node==null || !(node instanceof OntologyTermNode)) {
                        continue;
                    }   
                    log.debug("  Multi drop #{} - {} with parent {}",i,node.getDisplayName(),node.getParentNode().getDisplayName());
                    nodes.add(node);
                }
                else {
                    log.debug("Multi-transferable is expected to support OntologyTermNodeFlavor.");
                }
            }
            
            if (!nodes.isEmpty()) {
                return new OntologyTermPasteType(nodes, this, index);
            }
            
            return null;
        }
        else {
            log.debug("Transferable is expected to support either OntologyTermNodeFlavor or multiFlavor.");
            return null;
        }   
    }
    
    private class OntologyTermPasteType extends PasteType {
        
        private final List<OntologyTermNode> nodes;
        private final OntologyTermNode targetNode;
        private final int startingIndex;
        
        OntologyTermPasteType(List<OntologyTermNode> nodes, OntologyTermNode targetNode, int startingIndex) {
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
                OntologyTerm newParent = targetNode.getOntologyTerm();
                
                // Have to keep track of the original parents before we do anything, 
                // because once we start moving nodes, the parents will be recreated
                List<OntologyTerm> originalParents = new ArrayList<>();
                for(OntologyTermNode node : nodes) {
                    OntologyTermNode originalParentNode = (OntologyTermNode)node.getParentNode();
                    OntologyTerm originalParent = originalParentNode.getOntologyTerm();
                    originalParents.add(originalParent);
                }
                
                List<OntologyTerm> toAdd = new ArrayList<>();
                List<OntologyTermNode> toDestroy = new ArrayList<>();
                
                int i = 0;
                for(OntologyTermNode node : nodes) {

                    OntologyTerm ontologyTerm = node.getOntologyTerm();
                    
                    OntologyTerm originalParent = originalParents.get(i);
                    log.trace("{} has parent {}",newParent.getId(),originalParent.getId());

                    if (ontologyTerm.getId().equals(newParent.getId())) {
                        log.info("Cannot move a node into itself: {}",ontologyTerm.getId());
                        continue;
                    }
                    else if (newParent.hasChild(ontologyTerm)) {
                        log.info("Child already exists: {}",ontologyTerm.getId());
                        continue;
                    }
                    log.info("Pasting '{}' on '{}'",ontologyTerm.getName(),newParent.getName());
                    toAdd.add(ontologyTerm);
                    toDestroy.add(node);
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
                for(OntologyTermNode node : toDestroy) {
                    node.destroy();
                }
                
            } 
            catch (Exception e) {
                throw new IOException("Error pasting node",e);
            }
            return null;
        }
    }

    public static OntologyTermNode getOntologyTermNode(Transferable t) {
        OntologyTermNode node = null;
        try {
            node = (OntologyTermNode)t.getTransferData(OntologyTermNodeFlavor.SINGLE_FLAVOR);
        }
        catch (UnsupportedFlavorException | IOException e) {
            log.error("Error getting transfer data", e);
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    public static List<OntologyTermNode> getOntologyTermNodeList(Transferable t) {
        List<OntologyTermNode> node = null;
        try {
            node = (List<OntologyTermNode>)t.getTransferData(OntologyTermNodeFlavor.LIST_FLAVOR);
        }
        catch (UnsupportedFlavorException | IOException e) {
            log.error("Error getting transfer data", e);
        }
        return node;
    }
}
