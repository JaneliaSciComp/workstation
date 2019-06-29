package org.janelia.workstation.browser.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.api.actions.ContextualNodeActionTracker;
import org.janelia.workstation.browser.api.actions.NodeContext;
import org.janelia.workstation.browser.api.actions.ContextualNodeAction;
import org.janelia.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Actions",
        id = "ViewDetailsAction"
)
@ActionRegistration(
        displayName = "#CTL_ViewDetailsAction",
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Menu/Actions", position = 300),
    @ActionReference(path = "Shortcuts", name = "D-I")
})
@Messages("CTL_ViewDetailsAction=View Details")
public final class ViewDetailsAction extends AbstractAction implements ContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(ViewDetailsAction.class);
    private static final String NAME = NbBundle.getBundle(ViewDetailsAction.class).getString("CTL_ViewDetailsAction");

    private DomainObject selectedObject;

    public ViewDetailsAction() {
        super(NAME); // Setting name explicitly is necessary for eager actions
        setEnabled(false);
        ContextualNodeActionTracker.getInstance().register(this);
    }

    @Override
    public boolean enable(NodeContext nodeSelection) {
        this.selectedObject = null;
        if (nodeSelection.isSingleObjectOfType(DomainObject.class)) {
            this.selectedObject = nodeSelection.getSingleObjectOfType(DomainObject.class);
            log.debug("enabled for new viewer context: {}", selectedObject);
            setEnabled(true);
        }
        else {
            log.debug("disabled for new viewer context");
            setEnabled(false);
        }
        return isEnabled();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (selectedObject!=null) {
            new DomainDetailsDialog().showForDomainObject(selectedObject);
        }
    }
}
