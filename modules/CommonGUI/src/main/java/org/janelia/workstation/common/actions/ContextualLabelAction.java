package org.janelia.workstation.common.actions;

import java.util.Collection;


import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.interfaces.HasName;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(
        category = "actions",
        id = "ContextualLabelAction"
)
@ActionRegistration(
        displayName = "CTL_ContextualLabelAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/actions", position = 1)
})
@NbBundle.Messages("CTL_ContextualLabelAction=(Nothing selected)")
public class ContextualLabelAction extends BaseContextualNodeAction {

    public ContextualLabelAction() {
        setVisible(true);
    }

    @Override
    protected void processContext() {
    }

    @Override
    public String getName() {
        if (getNodeContext()==null || getNodeContext().getObjects().isEmpty()) {
            return super.getName();
        }
        Collection selectedObjects = getNodeContext().getObjects();
        if (selectedObjects.size()>1) {
            return "(Multiple selected)";
        }
        else {
            Object lastSelectedObject = getNodeContext().getObjects().iterator().next();
            if (lastSelectedObject != null) {
                String name;
                if (lastSelectedObject instanceof HasName) {
                    HasName named = (HasName) lastSelectedObject;
                    name = named.getName();
                }
                else {
                    name = lastSelectedObject.toString();
                }
                return StringUtils.abbreviate(name, 50);
            }
        }
        return super.getName();
    }

    @Override
    public void performAction() {
    }
}
