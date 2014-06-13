/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolConfigurationDialog;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Tools",
        id = "ToolConfigurationAction"
)
@ActionRegistration(
        displayName = "#CTL_ToolConfigurationAction"
)
@ActionReference(path = "Menu/Tools", position = 1200, separatorBefore = 1150)
@Messages("CTL_ToolConfigurationAction=Configure Tools")
public final class ToolConfigurationAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            JFrame parent = WindowLocator.getMainFrame();
            new ToolConfigurationDialog(parent);
        } catch ( Exception bse ) {
            SessionMgr.getSessionMgr().handleException( bse );
        }
    }
}
