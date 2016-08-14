package org.janelia.it.workstation.gui.browser.nb_action;

import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.openide.nodes.Node;

/**
 * This action allows the user to create new domain objects underneath an 
 * existing TreeNode, but right-clicking on it and choosing New from the 
 * context menu.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class NewDomainObjectAction extends NodePresenterAction {

    private final static NewDomainObjectAction singleton = new NewDomainObjectAction();
    public static NewDomainObjectAction get() {
        return singleton;
    }

    private NewDomainObjectAction() {
    }

    @Override 
    public JMenuItem getPopupPresenter() {

        List<Node> selected = getSelectedNodes();
        TreeNodeNode node = (TreeNodeNode)selected.get(0);
        
        JMenu newMenu = new JMenu("New");
        
        JMenuItem newFolderItem = new JMenuItem("Folder");
        newFolderItem.addActionListener(new NewFolderActionListener(node));
        newMenu.add(newFolderItem);
        
        JMenuItem newFilterItem = new JMenuItem("Filter");
        newFilterItem.addActionListener(new NewFilterActionListener(node));
        newMenu.add(newFilterItem);

        if (selected.size()!=1 || !ClientDomainUtils.hasWriteAccess(node.getTreeNode())) {
            newMenu.setEnabled(false);
        }
        
        return newMenu;
    }  
}
