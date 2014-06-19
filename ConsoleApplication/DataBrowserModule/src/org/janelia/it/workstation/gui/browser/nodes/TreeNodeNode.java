package org.janelia.it.workstation.gui.browser.nodes;

import com.google.common.collect.Lists;
import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.MaterializedView;
import org.janelia.it.jacs.model.domain.TreeNode;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.workstation.gui.browser.DomainDAO;
import org.janelia.it.workstation.gui.browser.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.children.TreeNodeChildFactory;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.Utils;
import org.openide.actions.CopyAction;
import org.openide.actions.CutAction;
import org.openide.actions.DeleteAction;
import org.openide.actions.MoveDownAction;
import org.openide.actions.MoveUpAction;
import org.openide.actions.PasteAction;
import org.openide.actions.RenameAction;
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
        super(parentChildFactory, treeNode);
        this.childFactory = new TreeNodeChildFactory(treeNode);
        Children lazyChildren = Children.create(childFactory, true);
        setChildren(lazyChildren);
        
        getCookieSet().add(new Index.Support() {

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
                Utils.runOffEDT(new Runnable() {
                    @Override
                    public void run() {
                        DomainDAO dao = DomainExplorerTopComponent.getDao();
                        dao.reorderChildren(SessionMgr.getSubjectKey(), getTreeNode(), order);
                    }
                },new Runnable() {
                    @Override
                    public void run() {
                        childFactory.refresh();
                    }
                });
            }
        });
    }
    
    private TreeNode getTreeNode() {
        return (TreeNode)getBean();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getTreeNode().getName();
    }
    
    @Override
    public Image getIcon(int type) {

        String typeSuffix = "";
        if (getTreeNode() instanceof MaterializedView) {
            if (getTreeNode().getName().equals(EntityConstants.NAME_DATA_SETS)) {
                typeSuffix = "_database";
            }
            else if (getTreeNode().getName().equals(EntityConstants.NAME_SHARED_DATA)) {
                typeSuffix = "_user";
            }
            else {
                typeSuffix = "_key";
            }
        }

        if (!getTreeNode().getOwnerKey().equals(SessionMgr.getSubjectKey())) {
            return Icons.getIcon("folder_blue"+typeSuffix+".png").getImage();
        }
        else {
            if (getTreeNode().getName().equals(EntityConstants.NAME_ALIGNMENT_BOARDS)) {
                return Icons.getIcon("folder_palette.png").getImage();
            }
            else {
                return Icons.getIcon("folder"+typeSuffix+".png").getImage();    
            }
        }
    }
    
    @Override
    public boolean canRename() {
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
        final TreeNode treeNode = getTreeNode();
        final String oldName = treeNode.getName();
        treeNode.setName(newName);
        Utils.runOffEDT(new Runnable() {
            public void run() {
                log.info("Changing name from " + oldName + " to: " + newName);
                DomainDAO dao = DomainExplorerTopComponent.getDao();
                dao.updateProperty(SessionMgr.getSubjectKey(), treeNode, "name", newName);
            }
        },new Runnable() {
            public void run() {
                log.info("Fire name change from" + oldName + " to: " + newName);
                fireDisplayNameChange(oldName, newName); 
            }
        });
    }

    @Override
    public PasteType getDropType(final Transferable t, int action, int index) {
        final TreeNode treeNode = getTreeNode();
        if (t.isDataFlavorSupported(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR)) {
            return new PasteType() {
                @Override
                public Transferable paste() throws IOException {
                    try {
                        DomainObject domainObject = (DomainObject) t.getTransferData(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR);
                        log.info("Pasting {} on {}",domainObject.getId(),treeNode.getName());
                        childFactory.addChild(domainObject);
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
            log.warn("Transfer does not support domain object flavor. It supports: "+t.getTransferDataFlavors());
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
