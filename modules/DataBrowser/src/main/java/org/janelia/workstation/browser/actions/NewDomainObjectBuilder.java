package org.janelia.workstation.browser.actions;

import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.workstation.browser.nb_action.NewFilterActionListener;
import org.janelia.workstation.browser.nb_action.NewFolderActionListener;
import org.janelia.workstation.browser.nodes.TreeNodeNode;
import org.janelia.workstation.common.nb_action.NodePresenterAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * This action allows the user to create new domain objects underneath an
 * existing TreeNode, but right-clicking on it and choosing New from the
 * context menu.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position = 120)
public class NewDomainObjectBuilder implements ContextualActionBuilder {

    private static final NewDomainObjectAction nodeAction = new NewDomainObjectAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof TreeNode;
    }

    @Override
    public Action getAction(Object obj) {
        return null;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return nodeAction;
    }

    public static class NewDomainObjectAction extends NodePresenterAction {

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

            if (selected.size()!=1 || !ClientDomainUtils.hasWriteAccess(node.getNode())) {
                newMenu.setEnabled(false);
            }

            return newMenu;
        }
    }
}
