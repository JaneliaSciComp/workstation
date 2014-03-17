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
        category = "Services",
        id = "org.janelia.it.FlyWorkstation.gui.framework.console.nb_action.DataSetsServiceAction"
)
@ActionRegistration(
        displayName = "#CTL_DataSetsServiceAction"
)
@ActionReference(path = "Menu/Services", position = 1200)
@Messages("CTL_DataSetsServiceAction=Data Sets")
public final class DataSetsServiceAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        new ServicesActionDelegate().presentDataSetListDialog();
    }
}
