package org.janelia.it.workstation.gui.framework.console.nb_action;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Help",
        id = "CallDeveloperMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_CallDeveloperMenuAction",
        lazy = false
)
@ActionReference(path = "Menu/Help", position = 100)
@Messages("CTL_CallDeveloperMenuAction=Contact A Developer")
public final class CallDeveloperMenuAction extends AbstractAction implements Presenter.Menu {

    private final JMenu subMenu = new JMenu("Contact A Developer");
    
    public CallDeveloperMenuAction() {
        subMenu.add(new JMenuItem("Call Christopher - x4662"));
        subMenu.add(new JMenuItem("Call Don    - x4656"));
        subMenu.add(new JMenuItem("Call Eric   - x4655"));
        subMenu.add(new JMenuItem("Call Konrad - x4242"));
        subMenu.add(new JMenuItem("Call Les    - x4680"));
        subMenu.add(new JMenuItem("Call Sean   - x4324"));
        subMenu.add(new JMenuItem("Call Todd   - x4696"));
        subMenu.add(new JMenuItem("Call Yang   - x4626"));

    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return subMenu;
    }
}
