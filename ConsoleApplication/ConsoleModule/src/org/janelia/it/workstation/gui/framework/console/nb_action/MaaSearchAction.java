/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
/*
NOTE: commented away these annotations.  This menu item is showing up
 for all users, but should not.  I do not know whether this is required to
 show for _any_ user. When I confirm it is not, this class may be deleted.

   fosterl@janelia.hhmi.org

@ActionID(
        category = "Search",
        id = "MaaSearchAction"
)
@ActionRegistration(
        displayName = "#CTL_MaaSearchAction"
)
@ActionReference(path = "Menu/Search", position = 1400)
@Messages("CTL_MaaSearchAction=MAA Screen Search")
*/
public final class MaaSearchAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        new SearchActionDelegate().maaSearch();
    }
}
