package org.janelia.it.workstation.gui.browser.nodes;


import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
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

public class TreeNodeNode extends DomainObjectNode {
    
    private final static Logger log = LoggerFactory.getLogger(TreeNodeNode.class);
    
    private final TreeNodeChildFactory childFactory;
    
    public TreeNodeNode(TreeNodeChildFactory parentChildFactory, TreeNode treeNode) throws Exception {
        this(parentChildFactory, treeNode.getNumChildren()==0?null:new TreeNodeChildFactory(treeNode), treeNode);
    }
    
    private TreeNodeNode(TreeNodeChildFactory parentChildFactory, final TreeNodeChildFactory childFactory, TreeNode treeNode) throws Exception {
        super(parentChildFactory, treeNode.getNumChildren()==0?Children.LEAF:Children.create(childFactory, true), treeNode);
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
                            DomainDAO dao = DomainExplorerTopComponent.getDao();
                            dao.reorderChildren(SessionMgr.getSubjectKey(), getTreeNode(), order);
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
        final TreeNode treeNode = getTreeNode();                
        if (t.isDataFlavorSupported(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR)) {
            try {
                DomainObject domainObject = (DomainObject) t.getTransferData(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR);
                log.info("Will paste {} on {}", domainObject.getId(), treeNode.getName());
            }
            catch (Exception ex) {
                log.error("WTF", ex);
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
