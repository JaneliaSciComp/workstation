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
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Search",
        id = "org.janelia.it.FlyWorkstation.gui.framework.console.nb_action.GlobalSOLrSearchAction"
)
@ActionRegistration(
        displayName = "#CTL_GlobalSOLrSearch"
)
@ActionReferences({
    @ActionReference(path = "Menu/Search", position = 1000),
    @ActionReference(path = "Shortcuts", name = "M-F")
})
@Messages("CTL_GlobalSOLrSearch=Search")
public final class GlobalSOLrSearchAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
    	new SearchActionDelegate().generalSearch();
    }
}
