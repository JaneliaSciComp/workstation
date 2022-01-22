package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.gui.cdmip.ColorDepthMask;
import org.janelia.workstation.colordepth.ColorDepthSearchDialog;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "AddToColorDepthSearchAction"
)
@ActionRegistration(
        displayName = "#CTL_AddToColorDepthSearchAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Sample", position = 580)
})
@NbBundle.Messages("CTL_AddToColorDepthSearchAction=Add Mask to Color Depth Search...")
public class AddToColorDepthSearchAction extends BaseContextualNodeAction {

    private ColorDepthMask selectedObject;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(ColorDepthMask.class)) {
            this.selectedObject = getNodeContext().getSingleObjectOfType(ColorDepthMask.class);
            setEnabled(ClientDomainUtils.isOwner(selectedObject));
            setVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        if (selectedObject != null) {
            new ColorDepthSearchDialog().showForMask(selectedObject);
        }
    }
}