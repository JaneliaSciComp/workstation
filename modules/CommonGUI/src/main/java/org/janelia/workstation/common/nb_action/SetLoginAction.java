package org.janelia.workstation.common.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.janelia.workstation.common.gui.dialogs.LoginDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.Actions;
import org.openide.awt.DynamicMenuContent;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File",
        id = "SetLoginAction"
)
@ActionRegistration(
        displayName = "#CTL_SetLoginAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1450, separatorBefore = 1425),
    @ActionReference(path = "Shortcuts", name = "A-o")
})
@Messages("CTL_SetLoginAction=Set Login")
public final class SetLoginAction extends AbstractAction implements DynamicMenuContent {

    @Override
    public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(() -> {
            LoginDialog.getInstance().showDialog();
        });
    }

    @Override
    public JComponent[] getMenuPresenters() {
        return createMenu();
    }

    @Override
    public JComponent[] synchMenuPresenters(JComponent[] jComponents) {
        return createMenu();
    }

    private JComponent[] createMenu() {
        if (Boolean.getBoolean("Login.Disabled")) {
            return new JComponent[0];
        } else {
            return new JComponent[] {
                    setLoginAction()
            };
        }
    }

    private static JMenuItem setLoginAction() {
        Action action = Actions.forID("File", "SetLoginAction");
        JMenuItem menuItem = new JMenuItem();
        menuItem.setText("Set Login");
        Actions.connect(menuItem, action, false);
        return menuItem;
    }
}
