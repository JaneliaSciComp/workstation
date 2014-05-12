/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.FlyWorkstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import org.janelia.it.FlyWorkstation.gui.dialogs.NewTiledMicroscopeSampleDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;

@ActionID(
        category = "File/New",
        id = "org.janelia.it.FlyWorkstation.gui.framework.console.nb_action.NewTiledMicroscopeSampleAction"
)
@ActionRegistration(
        displayName = "#CTL_NewTiledMicroscopeSampleAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File/New", position = 1400)
})
@Messages("CTL_NewTiledMicroscopeSampleAction=Add Tiled Microscope Sample")
public final class NewTiledMicroscopeSampleAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        menuNewTiledMicroscopeSample_actionPerformed();
    }
    
    private void menuNewTiledMicroscopeSample_actionPerformed() {
        JFrame parent = (JFrame)WindowManager.getDefault().getMainWindow();
        new NewTiledMicroscopeSampleDialog(parent, "Add Tiled Microscope Sample", true);
    }

}
