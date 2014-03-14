/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.FlyWorkstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File",
        id = "org.janelia.it.FlyWorkstation.gui.framework.console.nb_action.PrintScreenAction"
)
@ActionRegistration(
        displayName = "#CTL_PrintScreenAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1550, separatorBefore = 1525),
    @ActionReference(path = "Shortcuts", name = "p")
})
@Messages("CTL_PrintScreenAction=Print Screen")
public final class PrintScreenAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        filePrint_actionPerformed();
    }
    
    private void filePrint_actionPerformed() {
        Browser browser = SessionMgr.getBrowser();
        browser.printBrowser();
    }

}
