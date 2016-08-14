/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.browser.nb_action;

import org.janelia.it.workstation.gui.browser.ConsoleApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@ActionID(
        category = "Search",
        id = "GiantFiberPatternSearchAction"
)
@ActionRegistration(
        displayName = "#CTL_GiantFiberPatternSearchAction"
)
@ActionReference(path = "Menu/Search", position = 1300)
@Messages("CTL_GiantFiberPatternSearchAction=Giant Fiber Mask Search")
public final class GiantFiberPatternSearchAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        ConsoleApp.getConsoleApp().getGiantFiberSearchDialog().showDialog();
    }
}
