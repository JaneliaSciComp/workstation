package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.janelia.workstation.browser.actions.NewFilterActionListener;
import org.janelia.workstation.browser.actions.NewFolderActionListener;
import org.janelia.workstation.common.actions.BaseContextualPopupAction;
import org.janelia.workstation.common.nodes.TreeNodeNode;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * This action allows the user to create new domain objects underneath an
 * existing TreeNode, but right-clicking on it and choosing New from the
 * context menu.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "NewDomainObjectAction"
)
@ActionRegistration(
        displayName = "#CTL_NewDomainObjectAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 130)
})
@NbBundle.Messages("CTL_NewDomainObjectAction=New")
public class NewDomainObjectAction extends BaseContextualPopupAction {

    private TreeNodeNode parentNode;

    @Override
    protected void processContext() {
        this.parentNode = null;
        if (getNodeContext().isSingleNodeOfType(TreeNodeNode.class)) {
            this.parentNode = getNodeContext().getSingleNodeOfType(TreeNodeNode.class);
            setVisible(true);
            setEnabled(ClientDomainUtils.hasWriteAccess(parentNode.getNode()));
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    protected List<JComponent> getItems() {

        List<JComponent> items = new ArrayList<>();

        TreeNodeNode node = parentNode;

        JMenuItem newFolderItem = new JMenuItem("Folder");
        newFolderItem.addActionListener(new NewFolderActionListener(node));
        items.add(newFolderItem);

        JMenuItem newFilterItem = new JMenuItem("Filter");
        newFilterItem.addActionListener(new NewFilterActionListener(node));
        items.add(newFilterItem);

        return items;
    }
}
