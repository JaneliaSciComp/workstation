package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
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
import org.openide.nodes.BeanNode;
import org.openide.nodes.Children;
import org.openide.nodes.Index;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.PasteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rokickik
 */
public class TreeNodeNode extends BeanNode<TreeNode> {
    
    private Logger log = LoggerFactory.getLogger(TreeNodeNode.class);
    
    private TreeNodeChildFactory parentChildFactory;
    private TreeNodeChildFactory childFactory;
    
    public TreeNodeNode(TreeNodeChildFactory parentChildFactory, TreeNode treeNode) throws Exception {
        super(treeNode);
        this.parentChildFactory = parentChildFactory;
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
                        dao.reorderChildren(SessionMgr.getSubjectKey(), getBean(), order);
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
    
    @Override
    public boolean canCut() {
        return true;
    }

    @Override
    public boolean canCopy() {
        return true;
    }

    @Override
    public boolean canRename() {
        return true;
    }
    
    @Override
    public boolean canDestroy() {
        if (getBean() instanceof MaterializedView) {
            return false;
        }
        return true;
    }
    
    @Override
    public Action[] getActions(boolean context) {
        return new Action[]{
                    RenameAction.get(RenameAction.class),
                    CutAction.get(CutAction.class),
                    CopyAction.get(CopyAction.class),
                    PasteAction.get(PasteAction.class),
                    DeleteAction.get(DeleteAction.class),
                    MoveUpAction.get(MoveUpAction.class),
                    MoveDownAction.get(MoveDownAction.class)
                };
    }

    @Override
    public void setName(final String newName) {
        final TreeNode treeNode = getBean();
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
    public void destroy() throws IOException {
        if (parentChildFactory==null) {
            throw new IllegalStateException("Cannot destroy node without parent");
        }
        TreeNode treeNode = getBean();
        log.info("Destroying ", treeNode.getName());
        parentChildFactory.removeChild(treeNode);
    }

    @Override
    public Transferable clipboardCopy() throws IOException {
        log.info("clipboard COPY "+getBean());
        Transferable deflt = super.clipboardCopy();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR) {
            @Override
            protected TreeNode getData() {
                return (TreeNode)getBean();
            }
        });
        return added;
    }
    
    @Override
    public Transferable clipboardCut() throws IOException {
        log.info("clipboard CUT "+getBean());
        Transferable deflt = super.clipboardCut();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR) {
            @Override
            protected TreeNode getData() {
                return (TreeNode)getBean();
            }
        });
        return added;
    }
    
    @Override
    public PasteType getDropType(final Transferable t, int action, int index) {

        final TreeNode treeNode = getBean();
        log.info("getDropType for {} at index {} ",treeNode,index);
        
        if (t.isDataFlavorSupported(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR)) {
            return new PasteType() {
                @Override
                public Transferable paste() throws IOException {
                    try {
                        DomainObject domainObject = (DomainObject) t.getTransferData(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR);
                        log.info("Pasting {} on {}"+domainObject.getId(),treeNode.getName());
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
//        final TreeNode treeNode = getBean();
//        log.info("createPasteTypes called for {} with list: {} ",treeNode.getName(),s);
//        PasteType p = getDropType(t, 0, 0);
//        if (p != null) {
//            s.add(p);
//        }
    }
    
    @Override
    public Image getIcon(int type) {

        String typeSuffix = "";
        if (getBean() instanceof MaterializedView) {
            if (getBean().getName().equals(EntityConstants.NAME_DATA_SETS)) {
                typeSuffix = "_database";
            }
            else if (getBean().getName().equals(EntityConstants.NAME_SHARED_DATA)) {
                typeSuffix = "_user";
            }
            else {
                typeSuffix = "_key";
            }
        }

        if (!getBean().getOwnerKey().equals(SessionMgr.getSubjectKey())) {
            return Icons.getIcon("folder_blue"+typeSuffix+".png").getImage();
        }
        else {
            if (getBean().getName().equals(EntityConstants.NAME_ALIGNMENT_BOARDS)) {
                return Icons.getIcon("folder_palette.png").getImage();
            }
            else {
                return Icons.getIcon("folder"+typeSuffix+".png").getImage();    
            }
        }
    }
    
    @Override
    public String getHtmlDisplayName() {
        if (getBean() != null) {
            return "<font color='!Label.foreground'>" + getBean().getName() + "</font>" +
                    " <font color='#957D47'><i>" + getBean().getOwnerKey() + "</i></font>";
        } else {
            return null;
        }
    }
    
//    @Override
//    public Action[] getActions(boolean context) {
//        Action[] result = new Action[]{
//            new RefreshAction()
//        };
//        return result;
//    }
//
//    private final class RefreshAction extends AbstractAction {
//
//        public RefreshAction() {
//            putValue(Action.NAME, "Refresh");
//        }
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            //EntityExplorerTopComponent.refreshNode();
//        }
//    }
//    
//    

}
