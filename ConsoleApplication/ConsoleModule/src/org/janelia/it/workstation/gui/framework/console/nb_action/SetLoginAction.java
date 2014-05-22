/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import org.janelia.it.workstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.gui.util.panels.DataSourceSettingsPanel;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
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
public final class SetLoginAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        setLogin();
    }

    private void setLogin() {
        JFrame parent = WindowLocator.getMainFrame();
        PrefController.getPrefController().getPrefInterface(DataSourceSettingsPanel.class, parent);
    }

}
