package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "actions",
        id = "ViewDetailsAction"
)
@ActionRegistration(
        displayName = "#CTL_ViewDetailsAction",
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Menu/actions", position = 100, separatorBefore = 99),
    @ActionReference(path = "Shortcuts", name = "D-I")
})
@Messages("CTL_ViewDetailsAction=View Details")
public final class ViewDetailsAction extends BaseContextualNodeAction {

    private DomainObject selectedObject;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(DomainObject.class)) {
            this.selectedObject = getNodeContext().getSingleObjectOfType(DomainObject.class);
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        if (selectedObject!=null) {
            new DomainDetailsDialog().showForDomainObject(selectedObject);
        }
    }
}
