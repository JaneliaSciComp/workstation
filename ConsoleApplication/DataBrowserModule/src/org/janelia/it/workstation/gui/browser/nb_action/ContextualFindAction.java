package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.janelia.it.workstation.gui.browser.gui.find.FindContext;
import org.janelia.it.workstation.gui.browser.gui.find.FindContextManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Find",
        id = "ContextualFindAction"
)
@ActionRegistration(
        displayName = "#CTL_ContextualFindAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/Edit", position = 1000),
    @ActionReference(path = "Shortcuts", name = "M-F")
})
@Messages("CTL_ContextualFindAction=Find")
public final class ContextualFindAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        for(FindContext context : FindContextManager.getInstance().getActiveContexts()) {
            context.showFindUI();
        }
    }
}
