package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.controller.dialog.NewTiledMicroscopeSampleDialog;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(
        category = "Actions",
        id = "EditSamplePathAction"
)
@ActionRegistration(
        displayName = "#CTL_EditSamplePathAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Large Volume", position = 1520)
})
@NbBundle.Messages("CTL_EditSamplePathAction=Edit Sample Paths")
/**
 * Right-click context menu that allows user to edit a TmSample file path.
 */
public class EditSamplePathAction extends BaseContextualNodeAction {

    private TmSample sample;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(TmSample.class)) {
            sample = getNodeContext().getSingleObjectOfType(TmSample.class);
            setVisible(true);
            setEnabled(ClientDomainUtils.hasWriteAccess(sample));
        }
        else {
            sample = null;
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        NewTiledMicroscopeSampleDialog dialog = new NewTiledMicroscopeSampleDialog();
        dialog.showForSample(this.sample);
    }
}
