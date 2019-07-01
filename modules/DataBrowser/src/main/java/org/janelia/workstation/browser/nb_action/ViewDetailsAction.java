package org.janelia.workstation.browser.nb_action;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
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
    @ActionReference(path = "Menu/Actions", position = 100, separatorBefore = 99),
    @ActionReference(path = "Shortcuts", name = "D-I")
})
@Messages("CTL_ViewDetailsAction=View Details")
public final class ViewDetailsAction extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(ViewDetailsAction.class);
    private static final String NAME = NbBundle.getBundle(ViewDetailsAction.class).getString("CTL_ViewDetailsAction");

    private DomainObject selectedObject;

    public ViewDetailsAction() {
        super(NAME); // Setting name explicitly is necessary for eager actions
    }

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(DomainObject.class)) {
            this.selectedObject = getNodeContext().getSingleObjectOfType(DomainObject.class);
            log.debug("enabled for new viewer context: {}", getNodeContext());
            setVisible(true);
            setEnabled(true);
        }
        else {
            log.debug("disabled for new viewer context: {}", getNodeContext());
            setVisible(false);
            setEnabled(false);
        }
    }

    @Override
    public void performAction() {
        if (selectedObject!=null) {
            new DomainDetailsDialog().showForDomainObject(selectedObject);
        }
    }
}
