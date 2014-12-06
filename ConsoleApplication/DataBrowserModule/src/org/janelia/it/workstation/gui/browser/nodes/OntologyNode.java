package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.nodes.children.OntologyChildFactory;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.actions.RenameAction;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Index;
import org.openide.nodes.Node;
import org.openide.util.datatransfer.PasteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class OntologyNode extends DomainObjectNode {
    
    private final static Logger log = LoggerFactory.getLogger(OntologyNode.class);
    
    private final OntologyChildFactory childFactory;
    
    public OntologyNode(Ontology ontology) throws Exception {
        this(null, DomainUtils.isEmpty(ontology.getTerms()) ? null:new OntologyChildFactory(ontology, ontology), ontology);
    }
    
    private OntologyNode(ChildFactory parentChildFactory, final OntologyChildFactory childFactory, Ontology ontology) throws Exception {
        super(parentChildFactory, DomainUtils.isEmpty(ontology.getTerms()) ? Children.LEAF:Children.create(childFactory, true), ontology);
        this.childFactory = childFactory;
        if (!DomainUtils.isEmpty(ontology.getTerms())) {
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
                            DomainDAO dao = DomainExplorerTopComponent.getDao();
                            //dao.reorderChildren(SessionMgr.getSubjectKey(), getTreeNode(), order);
                        }
                        @Override
                        protected void hadSuccess() {
                            childFactory.refresh();
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
    
    private Ontology getOntology() {
        return (Ontology)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getOntology().getName();
    }
    
    @Override
    public String getSecondaryLabel() {
        return getOntology().getTypeName();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("folder.png").getImage();    
    }
    
    @Override
    public boolean canRename() {
        return true;
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
    
    @Override
    public Action[] getActions(boolean context) {
        Action[] superActions = super.getActions(context);
        List<Action> actions = new ArrayList<Action>();
        actions.add(RenameAction.get(RenameAction.class));
        actions.addAll(Lists.newArrayList(superActions));
        return actions.toArray(new Action[0]);
    }
    
    @Override
    public void setName(final String newName) {
        final Ontology ontology = getOntology();
        final String oldName = ontology.getName();
        ontology.setName(newName);
        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                log.trace("Changing name from " + oldName + " to: " + newName);
                DomainDAO dao = DomainExplorerTopComponent.getDao();
                dao.updateProperty(SessionMgr.getSubjectKey(), ontology, "name", newName);
            }
            @Override
            protected void hadSuccess() {
                log.trace("Fire name change from" + oldName + " to: " + newName);
                fireDisplayNameChange(oldName, newName); 
            }
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.execute();
    }

    @Override
    public PasteType getDropType(final Transferable t, int action, int index) {
        final Ontology ontology = getOntology();
        if (t.isDataFlavorSupported(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR)) {
            return new PasteType() {
                @Override
                public Transferable paste() throws IOException {
//                    try {
//                        DomainObject domainObject = (DomainObject) t.getTransferData(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR);
//                        log.info("Pasting {} on {}",domainObject.getId(),treeNode.getName());
//                        if (DomainUtils.hasChild(treeNode, domainObject)) {
//                            log.info("Child already exists. TODO: should reorder it to the end");
//                            return null;
//                        }
//                        childFactory.addChild(domainObject);
//                        final Node node = NodeTransfer.node(t, NodeTransfer.DND_MOVE + NodeTransfer.CLIPBOARD_CUT);
//                        if (node != null) {
//                            log.info("Original node was moved or cut, so we presume it was pasted, and will destroy node");
//                            node.destroy();
//                        }
//                    } catch (UnsupportedFlavorException ex) {
//                        log.error("Flavor is not supported for paste",ex);
//                    }
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
