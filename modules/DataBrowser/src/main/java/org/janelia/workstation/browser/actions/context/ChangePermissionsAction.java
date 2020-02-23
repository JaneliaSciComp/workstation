package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(
        category = "actions",
        id = "ChangePermissionsAction"
)
@ActionRegistration(
        displayName = "#CTL_ChangePermissionsAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/actions", position = 110)
})
@NbBundle.Messages("CTL_ChangePermissionsAction=Change Permissions")
public class ChangePermissionsAction extends BaseContextualNodeAction {

    private DomainObject selectedObject;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(DomainObject.class)) {
            this.selectedObject = getNodeContext().getSingleObjectOfType(DomainObject.class);
            setVisible(true);
            setEnabled(ClientDomainUtils.isOwner(selectedObject));
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        if (selectedObject != null) {
            new DomainDetailsDialog().showForDomainObject(selectedObject, DomainInspectorPanel.TAB_NAME_PERMISSIONS);
        }
    }
}