package org.janelia.it.workstation.gui.browser.nodes;


import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.nodes.children.ObjectSetChildFactory;
import org.janelia.it.workstation.gui.browser.nodes.children.TreeNodeChildFactory;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.nodes.Children;
import org.openide.nodes.Index;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.datatransfer.PasteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectSetNode extends DomainObjectNode {
    
    private final static Logger log = LoggerFactory.getLogger(ObjectSetNode.class);
    
    private final ObjectSetChildFactory childFactory;
    
    public ObjectSetNode(TreeNodeChildFactory parentChildFactory, ObjectSet objectSet) throws Exception {
        this(parentChildFactory, objectSet.getNumMembers()==0?null:new ObjectSetChildFactory(objectSet), objectSet);
    }
    
    private ObjectSetNode(TreeNodeChildFactory parentChildFactory, final ObjectSetChildFactory childFactory, ObjectSet objectSet) throws Exception {
        super(parentChildFactory, objectSet.getNumMembers()==0?Children.LEAF:Children.create(childFactory, true), objectSet);
        this.childFactory = childFactory;
        if (objectSet.getNumMembers()>0) {
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
                            // TODO: imple,ent for object sets
//                            DomainDAO dao = DomainExplorerTopComponent.getDao();
//                            dao.reorderChildren(SessionMgr.getSubjectKey(), getTreeNode(), order);
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
    
    public ObjectSet getObjectSet() {
        return (ObjectSet)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getObjectSet().getName();
    }
    
    @Override
    public Image getIcon(int type) {

        String typeSuffix = "";
        if (getObjectSet().getName().equals(EntityConstants.NAME_DATA_SETS)) {
            typeSuffix = "_database";
        }
        else if (getObjectSet().getName().equals(EntityConstants.NAME_SHARED_DATA)) {
            typeSuffix = "_user";
        }
        else {
            typeSuffix = "_key";
        }

        if (!getObjectSet().getOwnerKey().equals(SessionMgr.getSubjectKey())) {
            return Icons.getIcon("folder_blue"+typeSuffix+".png").getImage();
        }
        else {
            if (getObjectSet().getName().equals(EntityConstants.NAME_ALIGNMENT_BOARDS)) {
                return Icons.getIcon("folder_palette.png").getImage();
            }
            else {
                return Icons.getIcon("folder"+typeSuffix+".png").getImage();    
            }
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
    
//    @Override
//    public Action[] getActions(boolean context) {
//        Action[] superActions = super.getActions(context);
//        List<Action> actions = new ArrayList<Action>();
//        actions.add(RenameAction.get(RenameAction.class));
//        actions.addAll(Lists.newArrayList(superActions));
//        return actions.toArray(new Action[0]);
//    }
    
//    @Override
//    public void setName(final String newName) {
//        final TreeNode treeNode = getTreeNode();
//        final String oldName = treeNode.getName();
//        treeNode.setName(newName);
//
//        SimpleWorker worker = new SimpleWorker() {
//            @Override
//            protected void doStuff() throws Exception {
//                log.trace("Changing name from " + oldName + " to: " + newName);
//                DomainDAO dao = DomainExplorerTopComponent.getDao();
//                dao.updateProperty(SessionMgr.getSubjectKey(), treeNode, "name", newName);
//            }
//            @Override
//            protected void hadSuccess() {
//                log.trace("Fire name change from" + oldName + " to: " + newName);
//                fireDisplayNameChange(oldName, newName); 
//            }
//            @Override
//            protected void hadError(Throwable error) {
//                SessionMgr.getSessionMgr().handleException(error);
//            }
//        };
//        worker.execute();
//    }

    @Override
    public PasteType getDropType(final Transferable t, int action, int index) {
        final ObjectSet objectSet = getObjectSet();                
        if (t.isDataFlavorSupported(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR)) {
            try {
                DomainObject domainObject = (DomainObject) t.getTransferData(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR);
                log.info("Will paste {} on {}", domainObject.getId(), objectSet.getName());
            }
            catch (Exception ex) {
                log.error("WTF", ex);
            }
            return new PasteType() {
                @Override
                public Transferable paste() throws IOException {
//                    try {
//                        DomainObject domainObject = (DomainObject) t.getTransferData(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR);
//                        log.info("Pasting {} on {}",domainObject.getId(),objectSet.getName());
//                        if (DomainUtils.hasChild(objectSet, domainObject)) {
//                            log.info("Child already exists. TODO: should reorder it to the end");
//                        }
//                        else {
//                            childFactory.addChild(domainObject);    
//                        }
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
