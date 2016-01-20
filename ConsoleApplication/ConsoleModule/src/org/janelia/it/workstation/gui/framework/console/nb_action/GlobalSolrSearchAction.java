package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

//@ActionID(
//        category = "Search",
//        id = "GlobalSolrSearchAction"
//)
//@ActionRegistration(
//        displayName = "#CTL_GlobalSolrSearch"
//)
//@ActionReferences({
//    @ActionReference(path = "Menu/Search", position = 1000),
//    @ActionReference(path = "Shortcuts", name = "M-F")
//})
//@Messages("CTL_GlobalSolrSearch=Search")
public final class GlobalSolrSearchAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
    	new SearchActionDelegate().generalSearch();
    }
}
