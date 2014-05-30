/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File",
        id = "ImportAction"
)
@ActionRegistration(
        displayName = "#CTL_ImportAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1437, separatorAfter = 1443),
    @ActionReference(path = "Shortcuts", name = "A-I")
})
@Messages("CTL_ImportAction=Import Files")
public final class ImportAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        menuFileImport_actionPerformed();
    }
    
    private void menuFileImport_actionPerformed() {
        SessionMgr.getBrowser().getImportDialog().showDialog(null);
    }

}
