package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.janelia.it.workstation.gui.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.gui.browser.gui.editor.FilterEditorPanel;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File",
        id = "NewFilterAction"
)
@ActionRegistration(
        displayName = "#CTL_NewFilterAction"
)
@ActionReference(path = "Menu/File/New", position = 1)
@Messages("CTL_NewFilterAction=Filter")
public final class NewFilterAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {

        DomainListViewTopComponent browser = DomainListViewTopComponent.getActiveInstance();
        if (browser==null) {
            browser = new DomainListViewTopComponent();
            browser.open();
            browser.requestActive();
        }
        browser.setEditorClass(FilterEditorPanel.class);
        ((FilterEditorPanel)browser.getEditor()).loadNewFilter();
    }
}
