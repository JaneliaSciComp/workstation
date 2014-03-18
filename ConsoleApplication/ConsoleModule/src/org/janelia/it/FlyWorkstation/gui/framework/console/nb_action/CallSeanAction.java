/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.FlyWorkstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Help",
        id = "org.janelia.it.FlyWorkstation.gui.framework.console.nb_action.CallSeanAction"
)
@ActionRegistration(
        displayName = "#CTL_CallSeanAction"
)
@ActionReference(path = "Menu/Help", position = 1500)
@Messages("CTL_CallSeanAction=Call Sean        - x4324")
public final class CallSeanAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        new CallDeveloperDelegate().actOnCallDeveloper();
    }
}
