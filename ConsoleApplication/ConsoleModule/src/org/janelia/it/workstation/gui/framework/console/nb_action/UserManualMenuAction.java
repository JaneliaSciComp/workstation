/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;

@ActionID(
        category = "Help",
        id = "UserManualMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_UserGuideMenuAction"
)
@ActionReference(path = "Menu/Help", position = 120)
@Messages("CTL_UserGuideMenuAction=User Guide")
public final class UserManualMenuAction extends AbstractAction implements Presenter.Menu {

    JMenuItem userManual = new JMenuItem("User Manual");

    public UserManualMenuAction() {
        userManual.addActionListener(this);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(URI.create("http://wiki.int.janelia.org/wiki/display/JW/Introduction"));
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return userManual;
    }
}
