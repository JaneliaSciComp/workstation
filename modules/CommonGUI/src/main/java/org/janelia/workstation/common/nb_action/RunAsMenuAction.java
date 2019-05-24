package org.janelia.workstation.common.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import org.janelia.workstation.common.gui.dialogs.RunAsUserDialog;
import org.janelia.workstation.core.actions.PopupMenuGenerator;
import org.janelia.workstation.core.api.AccessManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

/**
 * Action for running as another user. This is separate from RunAsMenuActionBuilder because it allows us to
 * add a hotkey using the NetBeans action registration framework.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "File",
        id = "RunAsMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_RunAsMenuAction",
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "A-r")
})
@Messages("CTL_RunAsMenuAction=Run As")
public final class RunAsMenuAction extends AbstractAction implements Presenter.Menu, PopupMenuGenerator {

    private static final String NAME = NbBundle.getBundle(RunAsMenuAction.class).getString("CTL_RunAsMenuAction");

    private JMenuItem menuItem;

    RunAsMenuAction() {
        super(NAME);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (AccessManager.getAccessManager().isAdmin()) {
            RunAsUserDialog dialog = new RunAsUserDialog();
            dialog.showDialog();
        }
    }

    @Override
    public JMenuItem getMenuPresenter() {
        if (menuItem==null) {
            this.menuItem = new JMenuItem(NAME);
            menuItem.setAction(this);
        }
        return menuItem;
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return getMenuPresenter();
    }
}
