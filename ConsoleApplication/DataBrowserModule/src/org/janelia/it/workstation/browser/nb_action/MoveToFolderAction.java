package org.janelia.it.workstation.browser.nb_action;

import javax.swing.JMenuItem;

import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.it.workstation.browser.nodes.TreeNodeNode;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.domain.workspace.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Action to move currently selected nodes to a folder.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MoveToFolderAction extends AddToFolderAction {

    private final static Logger log = LoggerFactory.getLogger(MoveToFolderAction.class);

    private final static MoveToFolderAction singleton = new MoveToFolderAction();
    public static MoveToFolderAction get() {
        return singleton;
    }
    
    protected MoveToFolderAction() {
    }
    
    @Override
    public JMenuItem getPopupPresenter() {

        JMenuItem menu = super.getPopupPresenter();
        menu.setText("Move To Folder");

        for(org.openide.nodes.Node node : getSelectedNodes()) {
            if (node instanceof AbstractDomainObjectNode) {
                AbstractDomainObjectNode<?> selectedNode = (AbstractDomainObjectNode<?>)node;
                DomainObject domainObject = selectedNode.getDomainObject();
                if (!ClientDomainUtils.isOwner(domainObject)) {
                    menu.setEnabled(false);
                    break;
                }
            }
        }

        return menu;
    }

    @Override
    protected void addItemsToFolder(TreeNode folder, Long[] idPath) throws Exception {
        super.addItemsToFolder(folder, idPath);

        // TODO: ensure that all items were successfully added before deletion

        // Build list of things to remove
        Multimap<Node,DomainObject> removeMap = ArrayListMultimap.create();
        for(org.openide.nodes.Node node : getSelectedNodes()) {
            log.info("Moving selected node '{}' to folder '{}'",node.getDisplayName(),folder.getName());
            TreeNodeNode selectedNode = (TreeNodeNode)node;
            TreeNodeNode parentNode = (TreeNodeNode)node.getParentNode();
            if (ClientDomainUtils.isOwner(parentNode.getNode())) {
                removeMap.put(parentNode.getNode(), selectedNode.getNode());
            }
        }

        // Remove from existing folders
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        for(Node treeNode : removeMap.keys()) {
            model.removeChildren(treeNode, removeMap.get(treeNode));
        }
    }
}
