package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.it.jacs.model.domain.ontology.Category;
import org.janelia.it.jacs.model.domain.ontology.Custom;
import org.janelia.it.jacs.model.domain.ontology.EnumItem;
import org.janelia.it.jacs.model.domain.ontology.EnumText;
import org.janelia.it.jacs.model.domain.ontology.Interval;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.Tag;
import org.janelia.it.jacs.model.domain.ontology.Text;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.api.KeyBindings;
import org.janelia.it.workstation.gui.browser.components.OntologyExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.flavors.OntologyTermFlavor;
import org.janelia.it.workstation.gui.browser.flavors.OntologyTermNodeFlavor;
import org.janelia.it.workstation.gui.browser.gui.keybind.KeyboardShortcut;
import org.janelia.it.workstation.gui.browser.gui.keybind.KeymapUtil;
import org.janelia.it.workstation.gui.browser.nb_action.AddOntologyTermAction;
import org.janelia.it.workstation.gui.browser.nb_action.ApplyAnnotationAction;
import org.janelia.it.workstation.gui.browser.nb_action.OntologyExportAction;
import org.janelia.it.workstation.gui.browser.nb_action.OntologyImportAction;
import org.janelia.it.workstation.gui.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
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

    private final OntologyChildFactory parentChildFactory;
    private final OntologyChildFactory childFactory;
    private final Ontology ontology;
    
    public OntologyTermNode(OntologyChildFactory parentChildFactory, Ontology ontology, OntologyTerm ontologyTerm) {
        this(parentChildFactory, new OntologyChildFactory(ontology, ontologyTerm), ontology, ontologyTerm);    
    }
    
    private OntologyTermNode(OntologyChildFactory parentChildFactory, OntologyChildFactory childFactory, final Ontology ontology, OntologyTerm ontologyTerm) {
        super(parentChildFactory, childFactory.hasNodeChildren()?Children.create(childFactory, false):Children.LEAF, ontologyTerm);
        this.parentChildFactory = parentChildFactory;
        this.childFactory = childFactory;
        this.ontology = ontology;
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
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });
    }
    
    protected OntologyChildFactory getChildFactory() {
        return childFactory;
    }
    
    public OntologyTermNode getParent() {
        Node parent = getParentNode();
        return parent instanceof OntologyTermNode ? (OntologyTermNode)parent : null;
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
        return ontology;
    }
    
    public OntologyTerm getOntologyTerm() {
        return (OntologyTerm)getObject();
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
        OntologyNode ontologyNode = getOntologyNode();
        org.janelia.it.workstation.gui.framework.actions.Action action = ontologyNode.getActionForNode(this);
        if (action != null) {
            KeyboardShortcut bind = KeyBindings.getKeyBindings().getBinding(action);
            if (bind != null) {
                return "(" + KeymapUtil.getShortcutText(bind) + ")";
            }
        }
        return null;
    }
    
    @Override
    public Image getIcon(int type) {
        OntologyTerm term = getOntologyTerm();
        if (term instanceof Category) {
            return Icons.getIcon("folder.png").getImage();
        }
        else if (term instanceof org.janelia.it.jacs.model.domain.ontology.Enum) {
            return Icons.getIcon("folder_page.png").getImage();
        }
        else if (term instanceof Interval) {
            return Icons.getIcon("page_white_code.png").getImage();
        }
        else if (term instanceof Tag) {
            return Icons.getIcon("page_white.png").getImage();
        }
        else if (term instanceof Text) {
            return Icons.getIcon("page_white_text.png").getImage();
        }
        else if (term instanceof Custom) {
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
        List<Action> actions = new ArrayList<>();
        actions.add(PopupLabelAction.get());
        actions.add(null);
        actions.add(new InternalNode.CopyNameAction());
        actions.add(new CopyGUIDAction());
        actions.add(null);
        actions.add(OntologyImportAction.get());
        actions.add(OntologyExportAction.get());
        actions.add(null);
        actions.add(new AssignShortcutAction());
        actions.add(AddOntologyTermAction.get());
        actions.add(new RemoveAction());
        actions.add(null);
        actions.add(ApplyAnnotationAction.get());
        //actions.add(new RemoveAnnotationAction());
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public Action getPreferredAction() {
        return ApplyAnnotationAction.get();
    }
    
    protected final class CopyGUIDAction extends AbstractAction {

        public CopyGUIDAction() {
            putValue(NAME, "Copy GUID To Clipboard");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            HasIdentifier ontologyTerm = getLookup().lookup(HasIdentifier.class);
            if (ontologyTerm==null) {
                return;
            }
            Transferable t = new StringSelection(ontologyTerm.getId()+"");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
        }
    }
    
    protected final class AssignShortcutAction extends AbstractAction {

        public AssignShortcutAction() {
            putValue(NAME, "Assign Shortcut...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            OntologyNode ontologyNode = getOntologyNode();
            org.janelia.it.workstation.gui.framework.actions.Action action = ontologyNode.getActionForNode(OntologyTermNode.this);
            OntologyExplorerTopComponent explorer = OntologyExplorerTopComponent.getInstance();
            explorer.getKeyBindDialog().showForAction(action);            
        }
    }
    
    protected final class RemoveAction extends AbstractAction {

        public RemoveAction() {
            putValue(NAME, "Remove");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            String title;
            String msg;
            if (getOntologyTerm() instanceof Ontology) {
                title = "Delete Ontology";
                msg = "Are you sure you want to delete this ontology?";
            }
            else {
                title = "Delete Ontology Item";
                msg = "Are you sure you want to delete the item '"+getOntologyTerm().getName()+"' and all of its descendants?";   
            }
            
            int result = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(),
                    msg, title, JOptionPane.OK_CANCEL_OPTION);

            if (result != 0) return;
            
            try {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                if (getOntologyTerm() instanceof Ontology) {
                    model.removeOntology(getOntology().getId());   
                }
                else {
                    model.removeOntologyTerm(getOntology().getId(), getParent().getId(), getOntologyTerm().getId());    
                }
            }
            catch (Exception ex) {
                SessionMgr.getSessionMgr().handleException(ex);
            }
        }
    }
    
//    protected final class RemoveAnnotationAction extends AbstractAction {
//
//        public RemoveAnnotationAction() {
//            putValue(NAME, "Remove Annotation From Selected Items");
//        }
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            
//            OntologyTermNode node = OntologyTermNode.this;
//            OntologyTerm ontologyTerm = getLookup().lookup(OntologyTerm.class);
//            OntologyTermNode key = (ontologyTerm instanceof EnumItem) ? ((OntologyTermNode)node.getParentNode()) : node;
//            Long keyTermId = key.getId();
//            String keyTermValue = key.getDisplayName();
//            
//            log.info("Will remove annotation from all selected entities: {} ({})",keyTermValue,keyTermId);
//            
//            OntologyTermReference keyTermRef = new OntologyTermReference();
//            keyTermRef.setOntologyId(getOntology().getId());
//            keyTermRef.setOntologyTermId(key.getId());
//            
//            DomainListViewTopComponent listView = DomainListViewTopComponent.getActiveInstance();
//            if (listView==null || listView.getEditor()==null) return;
//            
//            DomainObjectSelectionModel selectionModel = listView.getEditor().getSelectionModel();
//            if (selectionModel==null) return;
//
//            // TODO: how to get image model?
//            ImageModel<DomainObject,DomainObjectId> imageModel = listView.getEditor().getImageModel();
//            if (imageModel==null) return;
//            
//            List<DomainObjectId> selectedIds = selectionModel.getSelectedIds();
//
//            DomainModel model = DomainMgr.getDomainMgr().getModel();
//            final List<DomainObject> selectedDomainObjects = model.getDomainObjectsByDomainObjectId(selectedIds);
//            
//            RemoveAnnotationTermAction<DomainObject,DomainObjectId> action = new RemoveAnnotationTermAction<>(imageModel, selectedDomainObjects, keyTermRef, keyTermValue);
//            action.doAction();
//        }
//    }

    @Override
    public void destroy() throws IOException {
        if (!ClientDomainUtils.hasWriteAccess(getOntology())) {
            return;
        }
        if (parentChildFactory==null) {
            throw new IllegalStateException("Cannot destroy node without parent");
        }
        if (parentChildFactory instanceof OntologyChildFactory) {
            OntologyChildFactory ontologyChildFactory = (OntologyChildFactory) parentChildFactory;
            try {
                ontologyChildFactory.removeChild(getOntologyTerm());
            }
            catch (Exception e) {
                throw new IOException("Error destroying node",e);
            }
        }
        else {
            throw new IllegalStateException("Cannot destroy term without parent");
        }
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
            OntologyTermNode node = OntologyTermNodeFlavor.getOntologyTermNode(t);
            if (node==null || !(node instanceof OntologyTermNode)) { 
                return null;
            }
            log.debug("  Single drop - {} with parent {}",node.getDisplayName(),node.getParentNode().getDisplayName());
            return new OntologyTermPasteType(Arrays.asList((OntologyTermNode)node), this, index);
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
                    OntologyTermNode node = OntologyTermNodeFlavor.getOntologyTermNode(st);
                    if (node==null || !(node instanceof OntologyTermNode)) {
                        continue;
                    }   
                    log.debug("  Multi drop #{} - {} with parent {}",i,node.getDisplayName(),node.getParentNode().getDisplayName());
                    nodes.add((OntologyTermNode)node);
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
}
