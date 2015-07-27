package org.janelia.it.workstation.gui.browser.nodes;


import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.nb_action.MoveToFolderAction;
import org.janelia.it.workstation.gui.browser.nb_action.NewDomainObjectAction;
import org.janelia.it.workstation.gui.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.gui.browser.nb_action.RemoveAction;
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

/**
 * A tree node (i.e. Folder) in the data graph.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TreeNodeNode extends DomainObjectNode {
    
    private final static Logger log = LoggerFactory.getLogger(TreeNodeNode.class);
    
    protected final TreeNodeChildFactory childFactory;
    
    public TreeNodeNode(TreeNodeChildFactory parentChildFactory, TreeNode treeNode) {
        this(parentChildFactory, new TreeNodeChildFactory(treeNode), treeNode);
    }
    
    protected TreeNodeNode(TreeNodeChildFactory parentChildFactory, final TreeNodeChildFactory childFactory, TreeNode treeNode) {
//        super(parentChildFactory, treeNode.getNumChildren()==0?Children.LEAF:Children.create(childFactory, false), treeNode);
        super(parentChildFactory, Children.create(childFactory, false), treeNode);
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
                            DomainDAO dao = DomainMgr.getDomainMgr().getDao();
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
        final TreeNode treeNode = getTreeNode();                
        if (t.isDataFlavorSupported(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR)) {
            
            final Node node = NodeTransfer.node(t, NodeTransfer.DND_MOVE + NodeTransfer.CLIPBOARD_CUT);
            final TreeNodeNode originalParentNode = (TreeNodeNode)node.getParentNode();
            //log.info("{} has parent {}",node,originalParentNode);
            
            return new PasteType() {
                @Override
                public Transferable paste() throws IOException {
                    if (node==null) {
                        throw new IOException("Cannot find node");
                    }   
                    try {
                        //TreeNodeNode originalParentNode = (TreeNodeNode)node.getParentNode();
                        log.info("{} has parent {}",node,originalParentNode);
                        
                        DomainObject domainObject = (DomainObject) t.getTransferData(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR);
                        if (domainObject.getId().equals(treeNode.getId())) {
                            log.info("Cannot move a node into itself");
                            return null;
                        }
                        log.info("Pasting {} on {}",domainObject.getId(),treeNode.getName());
                        if (DomainUtils.hasChild(treeNode, domainObject)) {
                            log.info("Child already exists.");
                        }
                        else {
                            childFactory.addChild(domainObject);
                            if (DomainUtils.hasWriteAccess(originalParentNode.getTreeNode())) {
                                log.info("Original node was moved or cut, so we presume it was pasted, and will destroy node");
                                node.destroy();
                            }
                        }
                    } 
                    catch (IOException e) {
                        throw e;
                    }
                    catch (Exception e) {
                        throw new IOException("Error pasting node",e);
                    }
                    return null;
                }
            };
        } 
        else {
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

    public void refresh() {
        childFactory.refresh();
    }
}
