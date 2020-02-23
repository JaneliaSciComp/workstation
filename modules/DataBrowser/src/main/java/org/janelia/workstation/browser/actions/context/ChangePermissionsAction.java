package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.workstation.browser.gui.dialogs.BulkChangePermissionDialog;
import org.janelia.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    private List<DomainObject> domainObjects = new ArrayList<>();

    @Override
    protected void processContext() {
        if (getNodeContext().isOnlyObjectsOfType(DomainObject.class)) {
            Collection<DomainObject> selectedObjects = getNodeContext().getOnlyObjectsOfType(DomainObject.class);
            setVisible(true);
            domainObjects.clear();
            boolean isAdmin = true;
            for (DomainObject object : selectedObjects) {
                domainObjects.add(object);
                if (!ClientDomainUtils.hasAdminAccess(object)) {
                    isAdmin = false;
                }
            }
            setEnabled(isAdmin);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public String getName() {
        return domainObjects.size() > 1 ? "Change Permissions for " + domainObjects.size() + " Items..." : "Change Permissions";
    }

    @Override
    public void performAction() {
        if (domainObjects.size()==1) {
            new DomainDetailsDialog().showForDomainObject(domainObjects.get(0), DomainInspectorPanel.TAB_NAME_PERMISSIONS);
        }
        else {
            new BulkChangePermissionDialog().showForDomainObjects(DomainUtils.getReferences(domainObjects), false);
        }
    }
}