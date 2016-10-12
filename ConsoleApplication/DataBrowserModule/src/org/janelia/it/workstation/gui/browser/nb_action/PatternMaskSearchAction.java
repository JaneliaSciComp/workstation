package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.janelia.it.workstation.gui.browser.ConsoleApp;

// TODO: this was never ported to NG.. it needs to be integrated with the domain model before it will work
//@ActionID(
//        category = "Search",
//        id = "PatternMaskSearchAction"
//)
//@ActionRegistration(
//        displayName = "#CTL_PatternMaskSearchAction"
//)
//@ActionReference(path = "Menu/Search", position = 1200)
//@Messages("CTL_PatternMaskSearchAction=Pattern Mask Search")
public final class PatternMaskSearchAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        ConsoleApp.getMaskSearchDialog().showDialog();
    }
}
