package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Help",
        id = "UserManualMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_UserGuideMenuAction",
        lazy = true
)
@ActionReference(path = "Menu/Help", position = 120)
@Messages("CTL_UserGuideMenuAction=User Manual")
public final class UserManualMenuAction extends AbstractAction implements Presenter.Menu {

    private static final Logger log = LoggerFactory.getLogger(UserManualMenuAction.class);
    
    private final JMenuItem userManual = new JMenuItem("User Manual");

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
            catch (IOException ex) {
                log.error("Could not open user manual URL",ex);
            }
        }
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return userManual;
    }
}
