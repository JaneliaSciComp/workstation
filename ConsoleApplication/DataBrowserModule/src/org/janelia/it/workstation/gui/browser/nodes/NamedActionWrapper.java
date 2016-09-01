package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.workstation.gui.browser.actions.NamedAction;

/**
 * An AbstractAction that wraps a NamedAction. The downside to having all these different ways of defining actions.
 */
public class NamedActionWrapper extends AbstractAction {

    private final NamedAction action;

    public NamedActionWrapper(NamedAction action) {
        this.action = action;
        putValue(NAME, action.getName().trim());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        action.doAction();
    }
}