package org.janelia.it.workstation.browser.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Search",
        id = "PatternAnnotationSearchAction"
)
@ActionRegistration(
        displayName = "#CTL_PatternAnnotationSearchAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/Search", position = 1100)
})
@Messages("CTL_PatternAnnotationSearchAction=Pattern Annotation Search")
public final class PatternAnnotationSearchAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        ConsoleApp.getConsoleApp().getPatternSearchDialog().showDialog();
    }
}
