package org.janelia.workstation.browser.actions.context;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.workstation.browser.actions.NewFilterActionListener;
import org.janelia.workstation.browser.actions.NewFolderActionListener;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.nodes.TreeNodeNode;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class NewDomainObjectAction extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(NewDomainObjectAction.class);
    private TreeNodeNode parentNode;

    @Override
    protected void processContext() {
        parentNode = null;
        if (getNodeContext().isSingleNodeOfType(TreeNodeNode.class)) {
            parentNode = getNodeContext().getSingleNodeOfType(TreeNodeNode.class);
            setVisible(true);
            setEnabled(ClientDomainUtils.hasWriteAccess(parentNode.getNode()));
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public JMenuItem getPopupPresenter() {

        if (!ContextualActionUtils.isVisible(this)) {
            return null;
        }

        TreeNodeNode node = parentNode;

        JMenu newMenu = new JMenu("New");

        JMenuItem newFolderItem = new JMenuItem("Folder");
        newFolderItem.addActionListener(new NewFolderActionListener(node));
        newMenu.add(newFolderItem);

        JMenuItem newFilterItem = new JMenuItem("Filter");
        newFilterItem.addActionListener(new NewFilterActionListener(node));
        newMenu.add(newFilterItem);

        newMenu.setEnabled(ContextualActionUtils.isEnabled(this));

        return newMenu;
    }

    @Override
    public void performAction() {
    }
}
